package org.apache.cordova.file;

import android.content.res.Resources.NotFoundException;
//import com.foreveross.chameleon.base.BslConfig;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * AES加密解密类,注这个类是与服务端搭配使用的 nodejs加密，这里解密，与JAVA加解密有一些区别
 *
 * @author wzq
 */
public class AesTool {
    private static final String AES_KEY = null;
    private static final String CHARSET_UTF8 = "UTF-8";
    private Cipher encryptCipher = null;// 加密
    private Cipher decryptCipher = null;// 解密

    private static AesTool mInstance = new AesTool();

    public static AesTool getInstance() {
        return mInstance;
    }

    private AesTool() {
        if (AES_KEY == null) {
            throw new RuntimeException("you must specify a AES key");
        }
        try {
            // key=>byte[] key => chipers
            byte[] raw = getByteKey(AES_KEY);
            init(raw);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private byte[] getByteKey(String key) {
        byte[] raw = null;
        try {
            byte[] keyb = key.getBytes(CHARSET_UTF8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            raw = md.digest(keyb);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return raw;
    }

    private void init(byte[] key) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            //
            encryptCipher = Cipher.getInstance("AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            //
            decryptCipher = Cipher.getInstance("AES");
            decryptCipher.init(Cipher.DECRYPT_MODE, skeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加密byte[]
     *
     * @return null if expception
     */
    public byte[] encryptBytes(byte[] bytes) {
        try {
            return encryptCipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密byte[]
     *
     * @return null if expception
     */
    public byte[] decryptBytes(byte[] src) {
        try {
            return decryptCipher.doFinal(src);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 加密String
     *
     * @return null if expception
     */
    public String encryptString(String str) {
        try {
            // 明文String => byte[] ==加密==> byte[] =>String
            byte[] srcBytes = str.getBytes(CHARSET_UTF8);
            byte[] dstBytes = encryptBytes(srcBytes);
            return bytes2String(dstBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密String,注意该String必须是对应加密方法得到到String
     *
     * @return null if expception
     */
    public String decryptString(String str) {
        // String => byte[] ==解密==> byte[] =>明文String
        try {
            byte[] encryptBytes = string2Bytes(str);
            byte[] decryptBytes = decryptBytes(encryptBytes);
            return new String(decryptBytes, CHARSET_UTF8);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 加密字节流
     *
     * @return null if expception
     */
    private InputStream encryptStream(InputStream in) {
        try {
            // InputStream => ByteArray => encrypt bytes => InputStream
            byte[] bytes = IOUtils.stream2Bytes(in);
            byte[] encryptBytes = encryptBytes(bytes);
            return IOUtils.bytes2Stream(encryptBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解密byteStream
     *
     * @return null if expception
     */
    public InputStream decryptStream(InputStream in) {
        try {
            // InputStream => ByteArray => decrypt bytes => InputStream
            byte[] bytes = IOUtils.stream2Bytes(in);
            byte[] decryptBytes = decryptBytes(bytes);
            return IOUtils.bytes2Stream(decryptBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 自定义的 byte[] => string,仅用于加密后的byte[] 保证String中只含有 0~F
     */
    private static String bytes2String(byte[] cryptBytes) {
        int len = cryptBytes.length;
        // byte 0xab => String型的:ab , 每个byte用2个字符表示
        StringBuffer sb = new StringBuffer(len * 2);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", cryptBytes[i]));
        }
        return sb.toString();
    }

    /**
     * 自定义的String => byte[],仅用于 {@link#bytes2String(byte[])}生成的String
     */
    private static byte[] string2Bytes(String str) {
        byte[] arrB = str.getBytes();
        int iLen = arrB.length;
        // 2个字符表示一个byte, 所以字节数组长度是字符串长度除以2
        byte[] arrOut = new byte[iLen / 2];
        for (int i = 0; i < iLen; i = i + 2) {
            String strTmp = new String(arrB, i, 2);
            arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
        }
        return arrOut;
    }

    /**
     * 解密文件
     *
     * @param fullPath 文件完整路径
     * @return 明文的InputStream, 如果解密失败则返回null
     * @throws java.io.IOException 文件不存在
     */
    public InputStream getFileAsStream(String fullPath) throws IOException {
        InputStream fileIn = new FileInputStream(fullPath);

        // 兼容处理:如果解密失败，则认为是未加密
        InputStream deStream = AesTool.getInstance().decryptStream(fileIn);
        fileIn.close();
        if (deStream != null) {
            return deStream;
        } else {
            return new FileInputStream(fullPath);
        }
    }

    // 对src文件加密，存放dst
    public void encryptFile(String src, String dst) throws IOException {
        File srcFile = new File(src);
        InputStream srcStream = new FileInputStream(srcFile);
        InputStream dstStream = encryptStream(srcStream);

        // 建立目录
        File dstFile = new File(dst);
        if (!dstFile.getParentFile().exists()) {
            dstFile.getParentFile().mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(dstFile);
        fos.write(IOUtils.stream2Bytes(dstStream));
        fos.flush();
        fos.close();
    }

    public void decryptFile(String src, String dst) throws IOException {
        File srcFile = new File(src);
        InputStream srcStream = new FileInputStream(srcFile);
        InputStream dstStream = decryptStream(srcStream);

        // 建立目录
        File dstFile = new File(dst);
        if (!dstFile.getParentFile().exists()) {
            dstFile.getParentFile().mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(dstFile);
        fos.write(IOUtils.stream2Bytes(dstStream));
        fos.flush();
        fos.close();
    }
}
