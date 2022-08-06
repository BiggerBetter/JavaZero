package Wanting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 1.target a folder find inside zip files
 * 2.目标zip文件如果是个文件夹就新建个文件夹，如果是文件就解压在当前目录
 * 3.if unzip process got a new folder, continue to unzip the new folder
 */
public class UnZipFolder {
    static List<String> folderList = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        String target = "/Users/Jenius/Desktop/new/";
        File sf = new File(target);
        if(sf.isDirectory()){
            folderList.add(sf.getPath());
        }else {
            decompress(sf.getPath());
        }

        while (!folderList.isEmpty()) {
            String tmpFolderPath = folderList.remove(0);
            if (!"".equals(tmpFolderPath)) {
                File file = new File(tmpFolderPath);
                File[] files = file.listFiles();
                assert files != null;
                for (File f : files) {
                    if (f.isDirectory()) {
                        folderList.add(f.getPath());
                    }else if(f.getName().endsWith(".zip")){
                        decompress(f.getPath());
                    }
                }
            }
        }
        listAllFileDeleteBlank(target);
        deleteAllZipFile(target);
    }

    /**
     * 解压文件/文件夹
     * srcPath：带文件名的路径
     * dest：目标文件夹，不带文件名
     */
    public static void decompress(String srcPath) throws Exception {
        File file = new File(srcPath);

        String dest = file.getParent();

        if (!file.exists()) {
            throw new RuntimeException(srcPath + "所指文件不存在");
        }

        ZipFile zf = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zf.entries();
        ZipEntry entry = null;
        while (entries.hasMoreElements()) {
            entry = (ZipEntry) entries.nextElement();
            System.out.println("解压" + entry.getName());
            if (entry.isDirectory()) {
                String dirPath = dest + File.separator + entry.getName();
                //add unzipped folder in to folderList
                folderList.add(dirPath);
                File dir = new File(dirPath);
                dir.mkdirs();
            } else {
                // 表示文件
                File f = new File(dest + File.separator + entry.getName());
                f.createNewFile();
                // 将压缩文件内容写入到这个文件中
                InputStream inStream = zf.getInputStream(entry);
                FileOutputStream outStream = new FileOutputStream(f);
                int count;
                byte[] buf = new byte[8192];
                while ((count = inStream.read(buf)) != -1) {
                    outStream.write(buf, 0, count);
                }
                inStream.close();
                outStream.close();
            }
            //delete unzipped file. PS: this delete doesnt influence outcome
            deleteFile(file.getPath());
        }
    }


    public static void listAllFileDeleteBlank(String path) {
        File f = new File(path);
        if(!f.isDirectory()){
            return;
        }
        File[] files = f.listFiles();
        for (File file : files) {
            System.out.println(file);
            if (file.isDirectory()) {
                if (file.getName().equals("__MACOSX")) {
                    deleteFile(file.getPath());
                    continue;
                }
                listAllFileDeleteBlank(file.getPath());
            }
        }
    }

    public static void deleteAllZipFile(String path) {
        File f = new File(path);
        if(!f.isDirectory()){
            return;
        }
        File[] files = f.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteAllZipFile(file.getPath());
            }else {
                if (file.getName().endsWith(".zip")) {
                    deleteFile(file.getPath());
                }
            }
        }
    }

    /**
     * 递归删除子文件
     *
     * @param path 文件路径
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] temp = file.listFiles(); //获取该文件夹下的所有文件
                assert temp != null;
                for (File value : temp) {
                    deleteFile(value.getAbsolutePath());
                }
            } else {
                //删除子文件
                file.delete();
            }
            //删除子文件
            file.delete();
        }
    }
}

