package org.dlut.adv.mineai.model.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.HardwareParams;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.SaveHardwareParamsDTO;
import org.dlut.adv.mineai.model.service.HardwareParamsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/hardwareParams")
@Api(tags = "硬件信息的请求控制器")
public class HardwareParamsController {

    @Resource
    private HardwareParamsService hardwareParamsService;

    /**
     * 保存硬件的参数
     *
     * @param saveHardwareParamsDTO 保存硬件参数的实体类
     * @return
     */
    @PostMapping("/saveHardwareParams")
    public Msg<String> saveHardwareParams(@RequestBody SaveHardwareParamsDTO saveHardwareParamsDTO) {
        HardwareParams hardwareParams = new HardwareParams();
        String cpu = saveHardwareParamsDTO.getCpus();
        String memory = saveHardwareParamsDTO.getMemory();
        String gpuMemory = saveHardwareParamsDTO.getGpuMemory();
        //该字段废弃，暂时置为1
        String vGpuCores = String.valueOf('1');

        hardwareParams.setCpus(cpu);
        hardwareParams.setMemory(memory + "Gi");
        hardwareParams.setGpuMemory(gpuMemory + "Gi");
        hardwareParams.setVGpuCores(vGpuCores);
        hardwareParams.setTitle(saveHardwareParamsDTO.getTitle());
        hardwareParams.setDescription(saveHardwareParamsDTO.getDescription());
        hardwareParams.setIsDefault(saveHardwareParamsDTO.getIsDefault());
        String name = cpu + "核" + memory + "G内存" + gpuMemory + "G显存";
        hardwareParams.setName(name);
        hardwareParamsService.saveHardwareParams(hardwareParams);
        return new Msg<>(MsgCode.SUCCEED);
    }


    /**
     * 按照硬件 id 获取硬件参数信息
     *
     * @param hardwareParamsId
     * @return
     */
    @GetMapping("/{hardwareParamsId}")
    @ApiOperation(value = "按照硬件 id 获取硬件参数信息")
    public Msg<HardwareParams> saveHardwareParams(@PathVariable Long hardwareParamsId) {
        HardwareParams hardwareParams = hardwareParamsService.findHardwareParamsById(hardwareParamsId);
        return new Msg<>(MsgCode.SUCCEED, hardwareParams);
    }
}