package org.dlut.adv.mineai.model.utils;

import org.dlut.adv.mineai.core.entity.ExternalModelConverter;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.model.domain.dto.ModelConvertDTO;
import org.dlut.adv.mineai.model.domain.entity.ModelConversionTask;
import org.dlut.adv.mineai.model.kubernetes.controller.JobController;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.service.ExternalModelConverterService;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ModelConversionQueueManager {

    private ModelJobRepo modelJobRepo;
    private JobController jobController;
    private ExternalModelConverterService externalModelConverterService;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Map<ExternalModelConverter, Queue<ModelConversionTask>> converterQueueMap = new HashMap<>();
    private Map<ExternalModelConverter, Integer> converterLoadMap = new HashMap<>();
    private Map<ExternalModelConverter, Boolean> isTaskRunningMap = new HashMap<>();


    public ModelConversionQueueManager(ModelJobRepo modelJobRepo, JobController jobController, ExternalModelConverterService externalModelConverterService) {
        this.modelJobRepo = modelJobRepo;
        this.jobController = jobController;
        this.externalModelConverterService = externalModelConverterService;

        // 初始化调度线程，定期检查所有转换器队列
        scheduler.scheduleAtFixedRate(this::processConversionTasks, 0, 5000, TimeUnit.MILLISECONDS);
    }

    // 添加转换任务到队列
    public void addConversionTask(ModelJob modelJob, ModelConvertDTO modelConvertDTO, String targetObjName, ExternalModelConverter selectedConverter) {
        if (selectedConverter != null) {
            ModelConversionTask task = new ModelConversionTask(modelJob, modelConvertDTO, targetObjName);
            Queue<ModelConversionTask> queue = converterQueueMap.computeIfAbsent(selectedConverter, k -> new LinkedList<>());
            queue.offer(task);
            updateConverterLoad(selectedConverter);

            // 确保初始状态下该转换器的运行状态已被跟踪
            isTaskRunningMap.putIfAbsent(selectedConverter, false);

            // 如果队列中只有一个任务（即当前任务），立即启动该任务
            if (queue.size() == 1 && !isTaskRunningMap.getOrDefault(selectedConverter, false)) {
                startTask(task, selectedConverter);
            }
        } else {
            throw new RuntimeException("没有找到合适的转换器");
        }
    }


    // 启动任务
    private void startTask(ModelConversionTask task, ExternalModelConverter converter) {
        System.out.println("启动任务: " + task.getModelJob().getName() + " 在转换器: " + converter.getIp());
        jobController.modelConvertStart(task.getModelConvertDTO(), task.getTargetObjName());
        isTaskRunningMap.put(converter, true);  // 标记转换器正在运行任务
    }


    // 定期检查任务队列的状态
    private void processConversionTasks() {
        for (Map.Entry<ExternalModelConverter, Queue<ModelConversionTask>> entry : converterQueueMap.entrySet()) {
            ExternalModelConverter converter = entry.getKey();
            Queue<ModelConversionTask> queue = entry.getValue();

            //printQueueStatus();


            // 检查当前转换器的任务队列
            if (!queue.isEmpty()) {
                ModelConversionTask currentTask = queue.peek();
                String name = currentTask.getModelJob().getName();
                ModelJob currentJob = modelJobRepo.findModelJobByName(name);

                if (isJobCompleted(currentJob)) {
                    // 完成任务，移除队列并更新负载
                    queue.poll();
                    updateConverterLoadOnFinish(converter);
                    isTaskRunningMap.put(converter, false);  // 标记任务已完成
                    // 启动下一个任务（如果有）
                    ModelConversionTask nextTask = queue.peek();
                    if (nextTask != null) {
                        startTask(nextTask, converter);
                        isTaskRunningMap.put(converter, true);  // 标记任务正在运行
                    }
                } else {
                    // 如果当前任务未完成，继续等待并标记转换器处于运行状态
                    isTaskRunningMap.put(converter, true);
                }
            }
        }
    }


    // 判断模型任务是否已完成
    private boolean isJobCompleted(ModelJob modelJob) {
        return modelJob.getStatus() == 6 || modelJob.getStatus() == -5 || modelJob.getStatus() == -10;
    }

    // 选择负载最小的转换器
    private ExternalModelConverter selectBestConverter() {
        return converterLoadMap.entrySet().stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // 更新转换器负载
    private void updateConverterLoad(ExternalModelConverter converter) {
        int load = converterLoadMap.getOrDefault(converter, 0);
        converterLoadMap.put(converter, load + 1);
    }

    // 任务完成后减少转换器的负载
    private void updateConverterLoadOnFinish(ExternalModelConverter converter) {
        int load = converterLoadMap.getOrDefault(converter, 0);
        if (load > 0) {
            converterLoadMap.put(converter, load - 1);
        }
    }

    // 打印队列状态
    public void printQueueStatus() {
        System.out.println("当前转换器队列状态：");
        converterQueueMap.forEach((converter, queue) -> {
            int load = converterLoadMap.getOrDefault(converter, 0);
            System.out.println("转换器：" + converter.getId() + " | IP：" + converter.getIp() + " | 当前负载：" + load);
            System.out.println("任务队列：");
            queue.forEach(task -> {
                System.out.println(" - 任务名称：" + task.getModelJob().getName() +
                        " | 目标对象：" + task.getTargetObjName() +
                        " | 状态：" + task.getModelJob().getStatus());
            });
        });
    }

    public Optional<ExternalModelConverter> findAndSelectConverter(String chipType) {
        List<ExternalModelConverter> allDevices = externalModelConverterService.findAllDevices();

        if (allDevices == null || allDevices.isEmpty()) {
            System.out.println("没有可用的 ExternalModelConverter 设备");
            return Optional.empty();
        }

        // 找到支持该 chipType 的所有转换器
        List<ExternalModelConverter> matchingConverters = allDevices.stream()
                .filter(device -> device.getChips().stream().anyMatch(chip -> chip.getChipType().equals(chipType)))
                .collect(Collectors.toList());

        if (matchingConverters.isEmpty()) {
            System.out.println("没有找到支持 chipType 为 " + chipType + " 的转换器");
        } else {
            System.out.println("找到 " + matchingConverters.size() + " 个支持 chipType 为 " + chipType + " 的转换器");
        }

        // 选择负载最小的转换器
        return matchingConverters.stream()
                .min(Comparator.comparingInt(converter -> converterLoadMap.getOrDefault(converter, 0)));
    }

    public int getTaskPositionInQueue(String modelJobName) {
        // 遍历所有转换器及其任务队列
        for (Map.Entry<ExternalModelConverter, Queue<ModelConversionTask>> entry : converterQueueMap.entrySet()) {
            Queue<ModelConversionTask> queue = entry.getValue();
            int position = 0;

            // 遍历队列查找指定的任务
            for (ModelConversionTask task : queue) {
                if (task.getModelJob().getName().equals(modelJobName)) {
                    return position; // 返回当前任务在队列中的位置
                }
                position++;
            }
        }
        // 如果未找到该任务，返回 -1 表示任务不在队列中
        return -1;
    }


    //0：不是集群外转换。1：正在运行的集群外转换。2：在队列的集群外转换
    public int isTaskInQueueOrRunning(String modelJobName) {
        //检查是不是在跑
        for (Map.Entry<ExternalModelConverter, Boolean> entry : isTaskRunningMap.entrySet()) {
            if (entry.getValue()) {
                Queue<ModelConversionTask> queue = converterQueueMap.get(entry.getKey());
                if (queue != null && !queue.isEmpty()) {
                    ModelConversionTask currentTask = queue.peek();
                    if (currentTask != null && currentTask.getModelJob().getName().equals(modelJobName)) {
                        return 1;
                    }
                }
            }
        }
        // 检查在不在队列
        for (Map.Entry<ExternalModelConverter, Queue<ModelConversionTask>> entry : converterQueueMap.entrySet()) {
            Queue<ModelConversionTask> queue = entry.getValue();
            for (ModelConversionTask task : queue) {
                if (task.getModelJob().getName().equals(modelJobName)) {
                    return 2;
                }
            }
        }


        return 0;
    }


    public boolean removeTaskFromQueue(String modelJobName) {
        boolean isRemoved = false;

        // 遍历所有转换器队列
        for (Map.Entry<ExternalModelConverter, Queue<ModelConversionTask>> entry : converterQueueMap.entrySet()) {
            ExternalModelConverter converter = entry.getKey();
            Queue<ModelConversionTask> queue = entry.getValue();

            // 检查当前任务是否正在运行
            if (isTaskRunningMap.getOrDefault(converter, false)) {
                // 获取当前正在运行的任务
                ModelConversionTask currentTask = queue.peek();
                if (currentTask != null && currentTask.getModelJob().getName().equals(modelJobName)) {
                    // 如果是正在运行的任务
                    System.out.println("任务正在运行，移除任务: " + modelJobName);
                    queue.poll(); // 从队列中移除正在运行的任务
                    // 更新任务状态为未运行
                    isTaskRunningMap.put(converter, false);
                    // 更新负载
                    updateConverterLoadOnFinish(converter);

                    // 启动下一个任务（如果有）
                    ModelConversionTask nextTask = queue.peek();
                    if (nextTask != null) {
                        startTask(nextTask, converter);
                        isTaskRunningMap.put(converter, true);  // 标记任务正在运行
                    }
                    isRemoved = true;

                    break;
                }
            }
            // 如果任务不在运行，检查队列中是否存在
            Iterator<ModelConversionTask> iterator = queue.iterator();
            while (iterator.hasNext()) {
                ModelConversionTask task = iterator.next();
                if (task.getModelJob().getName().equals(modelJobName)) {
                    System.out.println("从队列中移除任务: " + modelJobName);
                    // 从队列移除该任务
                    iterator.remove();
                    // 更新负载
                    updateConverterLoadOnFinish(converter);
                    isRemoved = true;
                    break;
                }
            }

            if (isRemoved) {
                break; // 如果任务已移除，跳出外层循环
            }
        }

        if (!isRemoved) {
            System.out.println("未找到任务: " + modelJobName);
        }

        return isRemoved;
    }


}
