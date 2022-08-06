package Wanting;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.*;

public class UnZipFolder {
    public static void main(String[] args) throws Exception {
        List<String> folderList = new ArrayList<>();
        folderList.add("/Users/Jenius/Desktop/unzip2/");

        String srcPath = "/Users/Jenius/Desktop/unzip2/中文.pptx.zip";
        String dest = "/Users/Jenius/Desktop/unzip2/中文.pptx";
        decompress(srcPath,dest);
    }
    /**
     * 解压文件/文件夹
     */
    public static void decompress(String srcPath, String dest) throws Exception {
        File file = new File(srcPath);
        if (!file.exists()) {
            throw new RuntimeException(srcPath + "所指文件不存在");
        }
        ZipFile zf = new ZipFile(file);
        Enumeration entries = zf.entries();
        ZipEntry entry = null;
        while (entries.hasMoreElements()) {
            entry = (ZipEntry) entries.nextElement();
            System.out.println("解压" + entry.getName());
            if (entry.isDirectory()) {
                String dirPath = dest + File.separator + entry.getName();
                File dir = new File(dirPath);
                dir.mkdirs();
            } else {
                // 表示文件
                File f = new File(dest + File.separator + entry.getName());
                if (!f.exists()) {
                    //String dirs = FileUtils.getParentPath(f);
                    String dirs = f.getParent();
                    File parentDir = new File(dirs);
                    parentDir.mkdirs();
                }
                f.createNewFile();
                // 将压缩文件内容写入到这个文件中
                InputStream is = zf.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(f);
                int count;
                byte[] buf = new byte[8192];
                while ((count = is.read(buf)) != -1) {
                    fos.write(buf, 0, count);
                }
                is.close();
                fos.close();
            }
        }
    }


    /**
     * 清理所有的__MACOSX文件夹
     */
}

