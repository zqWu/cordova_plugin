package org.apache.cordova.file;

import java.io.*;

/**
 * byte[] <=> Stream <=> String File 之间的相互转换用到的工具类
 *
 * @author wzq
 * @2014年5月5日
 */
public class IOUtils {
    /**
     * 打印一个byte[]
     */
    public static String printByteArray(byte[] bytes) {
        StringBuilder sb = new StringBuilder(1024);
        for (byte b : bytes) {
            sb.append(printByte(b)).append(" ");
        }
        return sb.toString();
    }

    /**
     * 供测试,打印一个byte,如 0xab 的打印结果 => ab
     */
    public static String printByte(byte b) {
        final String BYTE_FORMAT = "%02x";
        return String.format(BYTE_FORMAT, b);
    }

    /**
     * InputStream => String
     *
     * @param in
     * @param encoding null= "UTF-8"
     * @return
     * @throws java.io.IOException
     */
    public static String stream2String(InputStream in, String encoding) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final int BUFFER_SIZE = 1024;
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, data.length)) != -1) {
            outStream.write(data, 0, count);
        }
        data = null;
        if (encoding == null) {
            encoding = "UTF-8";
        }
        return new String(outStream.toByteArray(), encoding);
    }

    /**
     * inputStream => byte[]
     *
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static byte[] stream2Bytes(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        final int BUFFER_SIZE = 1024;
        byte[] data = new byte[BUFFER_SIZE];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        byte[] bytes = buffer.toByteArray();
        return bytes;
    }

    public static void stream2File(InputStream in, String fullPath) throws IOException {
        File file = new File(fullPath);
        stream2File(in, file);
    }

    public static void stream2File(InputStream in, File file) throws IOException {
        // mkdirs
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        //
        OutputStream os = new FileOutputStream(file);
        int bytesRead = 0;
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
        os.close();
        in.close();
    }

    /**
     * byte[] => file
     *
     * @param bytes
     * @param file  存储的路径名称
     * @throws java.io.IOException
     */
    public static void bytes2File(byte[] bytes, File file) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bos.write(bytes);
        bos.flush();
        bos.close();
    }

    /**
     * file => byte[]
     *
     * @param file
     * @throws java.io.IOException
     */
    public static byte[] file2Bytes(File file) throws IOException {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
        buf.read(bytes, 0, bytes.length);
        buf.close();
        return bytes;
    }

    /**
     * byte[] => InputStream
     *
     * @param in
     * @throws Exception
     */
    public static InputStream bytes2Stream(byte[] in) throws IOException {
        if (in == null) {
            return null;
        }
        ByteArrayInputStream is = new ByteArrayInputStream(in);
        return is;
    }

    /**
     * byte[] => String
     *
     * @param bytes
     * @param encoding null=utf-8
     * @throws java.io.IOException
     */
    public static String bytes2String(byte[] bytes, String encoding) throws IOException {
        if (bytes == null) {
            return null;
        }
        InputStream is = bytes2Stream(bytes);
        if (encoding == null) {
            encoding = "UTF-8";
        }
        return stream2String(is, encoding);
    }

    public static byte[] string2Bytes(String str, String encoding) throws IOException {
        if (str == null) {
            return null;
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        return str.getBytes(encoding);
    }


    public static InputStream string2Stream(String str, String encoding) throws IOException {
        if (str == null) {
            return null;
        }
        byte[] bytes = string2Bytes(str, encoding);
        return bytes2Stream(bytes);
    }

    /**
     * 拷贝byte数组
     */
    public static byte[] copyBytes(byte[] src) {
        byte[] copy = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i];
        }
        return copy;
    }

    /**
     * 获取一个流的部分
     *
     * @param is
     * @param start 起始位置 include
     * @param end   结束位置 exclude -1表示到结尾
     * @return the substream
     * @throws java.io.IOException
     */
    public static byte[] subStream(InputStream is, long start, long end) throws IOException {
        int begin = (int) start;
        int stop = (int) end;

        if (begin < 0) {
            throw new RuntimeException("get subStream err: start < 0");
        }
        byte[] bytes = stream2Bytes(is);
        if (stop > bytes.length) {
            throw new RuntimeException("get subStream err: end > length");
        }
        if (stop == -1) {
            stop = bytes.length;
        }
        int len = stop - begin;
        byte[] bytes1 = new byte[len];
        for (int i = begin; i < len; i++) {
            bytes1[i - begin] = bytes[i];
        }
        return bytes1;
    }

    public static InputStream combine(InputStream src, byte[] bytes) throws IOException {
        byte[] bytes1 = IOUtils.stream2Bytes(src);
        int len = bytes1.length + bytes.length;
        byte[] bytes2 = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes2[i] = i < bytes1.length ? bytes1[i] : bytes[i - bytes1.length];
        }
        return bytes2Stream(bytes2);
    }
}
