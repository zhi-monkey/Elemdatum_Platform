package org.dlut.adv.mineai.core.utils;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author wwwgl
 */
public class FileUtil {
    /**
     * 移动文件
     *
     * @param oldPath
     * @param trgFolder
     * @return boolean
     */
    public static boolean moveFile(String oldPath, String trgFolder, String newName) {
        File file = new File(oldPath);
        if(!createFolder(trgFolder)){
            return false;
        }
        if (file.exists()) {
            System.err.println("file存在");
            if(file.isFile()){
                System.err.println("是文件");
                String destinationFile = trgFolder + File.separator + newName;
                return file.renameTo(new File(destinationFile));
            }else {
                System.err.println("不是文件");
                return false;
            }
        } else {
            System.err.println("被转移文件不存在");
            return false;
        }
    }

    /**
     * 根据路径创建文件夹，如果路径为文件或文件夹创建失败则返回false
     * @param trgFolder
     * @return
     */
    private static boolean createFolder(String trgFolder){
        File destFolder = new File(trgFolder);
        Path destPath = Paths.get(trgFolder);
        if (destFolder.exists()) {
            if (destFolder.isFile()) {
                System.err.println("实际是文件");
                return false;
            }else {
                System.err.println("文件夹存在");
                return true;
            }
        } else {
            try {
                System.err.println("开始创建文件夹");
                Files.createDirectories(destPath);
                return true;
            } catch (IOException e) {
                System.err.println("文件夹创建失败");
                e.printStackTrace();
                return false;
            }
        }
    }


    /**
     * 计算文件夹下文件总数
     *
     * @param folderPath
     * @return
     */
    public static long countFileNumOfFolder(String folderPath) {
        long num = 0;
        File folder = new File(folderPath);
        if (folder.isFile() || !folder.exists()) {
            return num;
        }
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile()) {
                num++;
            } else if (file.isDirectory()) {
                num += countFileNumOfFolder(file.getAbsolutePath());
            }
        }
        return num;
    }

    /**
     * 删除文件
     *
     * @param fileName
     * @return
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        // 如果文件路径只有单个文件
        if (!file.exists()) {
            return true;
        }
        return file.delete();
    }

    /**
     * 删除文件夹
     *
     * @param trgPath
     * @return
     */
    public static boolean deleteFolder(String trgPath) {
        File trgFolder = new File(trgPath);
        if (!trgFolder.exists()) {
            return true;
        }
        if (trgFolder.isFile()) {
            return false;
        }
        for (File file : trgFolder.listFiles()) {
            if (file.isDirectory()) {
                if (!deleteFolder(file.getAbsolutePath())) {
                    return false;
                }
            } else if (file.isFile()) {
                if (!deleteFile(file.getAbsolutePath())) {
                    return false;
                }
            }
        }
        return trgFolder.delete();
    }
    /**
     * 复制文件
     *
     * @param srcFile
     * @param trgFile
     */
    public static boolean copyFile(File srcFile, File trgFile) {
        //定义出字节缓冲流
        BufferedInputStream BufIn = null;
        BufferedOutputStream Bufout = null;
        try {
            BufIn = new BufferedInputStream(Files.newInputStream(srcFile.toPath()));
            Bufout = new BufferedOutputStream(Files.newOutputStream(trgFile.toPath()));
            int BufRead;
            while ((BufRead = BufIn.read()) != -1) {
                Bufout.write(BufRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (BufIn != null) {
                    BufIn.close();
                }
                if (Bufout != null) {
                    Bufout.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    /**
     * 划分文件夹中的文件，如果子文件夹已经存在会返回false，基本逻辑是计算划分数量n，按顺序遍历，前n个分给A数据集，其余给B
     * @param divideSize 划分比例
     * @return
     */
    public static boolean folderDivide(double divideSize, String folderSrc, String trgA, String trgB){
        File srcFolder = new File(folderSrc);
        if(!createFolder(trgA) || !createFolder(trgB) || !srcFolder.exists() || srcFolder.isFile()){
            return false;
        }
        //需要复制到文件夹A的数量
        long numOfTrgA = (long) (countFileNumOfFolder(folderSrc) * divideSize);
        //已经复制的数量
        long numCopied = 0;
        for (File f : srcFolder.listFiles()){
            //前numOfTrgA个文件放入A文件夹，其余的放入B文件夹
            if(numCopied < numOfTrgA){
                if(f.isFile()){
                    File trgFile = new File(trgA + File.separator + f.getName());
                    if(!copyFile(f, trgFile)){
                        deleteFolder(trgA);
                        deleteFolder(trgB);
                        return false;
                    }
                    numCopied ++;
                }else {
                    deleteFolder(trgA);
                    deleteFolder(trgB);
                    return false;
                }
            }
            else {
                if(f.isFile()){
                    File trgFile = new File(trgB + File.separator + f.getName());
                    if(!copyFile(f, trgFile)){
                        deleteFolder(trgA);
                        deleteFolder(trgB);
                        return false;
                    }
                }else {
                    deleteFolder(trgA);
                    deleteFolder(trgB);
                    return false;
                }
            }
        }
        return true;
    }
}
