package utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class KoalasRsaUtil {

    /** */
    /**
     * 加密算法RSA
     */
    public static final String KEY_ALGORITHM = "RSA";

    /** */
    /**
     * 签名算法
     */
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    /** */
    /**
     * 获取公钥的key
     */
    private static final String PUBLIC_KEY = "RSAPublicKey";

    /** */
    /**
     * 获取私钥的key
     */
    private static final String PRIVATE_KEY = "RSAPrivateKey";

    /** */
    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /** */
    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /** */
    /**
     * <p>
     * 生成密钥对(公钥和私钥)
     * </p>
     *
     * @return
     * @throws Exception
     */
    public static Map<String, Object> genKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance ( KEY_ALGORITHM );
        keyPairGen.initialize ( 1024 );
        KeyPair keyPair = keyPairGen.generateKeyPair ();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic ();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate ();
        Map<String, Object> keyMap = new HashMap<String, Object> ( 2 );
        keyMap.put ( PUBLIC_KEY, publicKey );
        keyMap.put ( PRIVATE_KEY, privateKey );
        return keyMap;
    }

    /** */
    /**
     * <p>
     * 用私钥对信息生成数字签名
     * </p>
     *
     * @param data       已加密数据
     * @param privateKey 私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static String sign(byte[] data, String privateKey) throws Exception {
        byte[] keyBytes = Base64.decodeBase64 ( privateKey.getBytes ( "UTF-8" ) );
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec ( keyBytes );
        KeyFactory keyFactory = KeyFactory.getInstance ( KEY_ALGORITHM );
        PrivateKey privateK = keyFactory.generatePrivate ( pkcs8KeySpec );
        Signature signature = Signature.getInstance ( SIGNATURE_ALGORITHM );
        signature.initSign ( privateK );
        signature.update ( data );
        return new String ( Base64.encodeBase64 ( signature.sign () ), "UTF-8" );
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> map1 = KoalasRsaUtil.genKeyPair ();
        Map<String, Object> map2 = KoalasRsaUtil.genKeyPair ();

        String clientprivateKey1 =  getPrivateKey(map1);
        String clientpublicKey2 = getPublicKey (map2);

        String serverprivateKey2 =  getPrivateKey(map2);
        String serverpublicKey1 = getPublicKey (map1);

        System.out.println ("下面四个字符串为koalas-rpc中客户端和服务端使用的rsa非对称秘钥，复制使用即可");

        //client
        System.out.println (clientprivateKey1);
        System.out.println (clientpublicKey2);

        //server
        System.out.println (serverprivateKey2);
        System.out.println (serverpublicKey1);
        System.out.println ("上面四个字符串为koalas-rpc中客户端和服务端使用的rsa非对称秘钥，复制使用即可");

        String body = "你好";

        byte[] enbody =encryptByPublicKey(body.getBytes ( "UTF-8" ),clientpublicKey2);
        String sign =sign(enbody,clientprivateKey1);

        System.out.println (sign);
        System.out.println (verify(enbody,serverpublicKey1,sign));

        System.out.println (new String(decryptByPrivateKey (enbody, serverprivateKey2)));

    }

    /** */
    /**
     * <p>
     * 校验数字签名
     * </p>
     *
     * @param data      已加密数据
     * @param publicKey 公钥(BASE64编码)
     * @param sign      数字签名
     * @return
     * @throws Exception
     */
    public static boolean verify(byte[] data, String publicKey, String sign)
            throws Exception {
        byte[] keyBytes = Base64.decodeBase64 ( publicKey.getBytes ("UTF-8") );
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec ( keyBytes );
        KeyFactory keyFactory = KeyFactory.getInstance ( KEY_ALGORITHM );
        PublicKey publicK = keyFactory.generatePublic ( keySpec );
        Signature signature = Signature.getInstance ( SIGNATURE_ALGORITHM );
        signature.initVerify ( publicK );
        signature.update ( data );
        return signature.verify ( Base64.decodeBase64 ( sign.getBytes ("UTF-8") ) );
    }

    /** */
    /**
     * <P>
     * 私钥解密
     * </p>
     *
     * @param encryptedData 已加密数据
     * @param privateKey    私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPrivateKey(byte[] encryptedData, String privateKey)
            throws Exception {
        byte[] keyBytes = Base64.decodeBase64 ( privateKey.getBytes ( "UTF-8" ) );

        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec ( keyBytes );
        KeyFactory keyFactory = KeyFactory.getInstance ( KEY_ALGORITHM );
        Key privateK = keyFactory.generatePrivate ( pkcs8KeySpec );
        Cipher cipher = Cipher.getInstance ( keyFactory.getAlgorithm () );
        cipher.init ( Cipher.DECRYPT_MODE, privateK );
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密  
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal ( encryptedData, offSet, MAX_DECRYPT_BLOCK );
            } else {
                cache = cipher.doFinal ( encryptedData, offSet, inputLen - offSet );
            }
            out.write ( cache, 0, cache.length );
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray ();
        out.close ();
        return decryptedData;
    }

    /** */
    /**
     * <p>
     * 公钥解密
     * </p>
     *
     * @param encryptedData 已加密数据
     * @param publicKey     公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPublicKey(byte[] encryptedData, String publicKey)
            throws Exception {
        byte[] keyBytes = Base64.decodeBase64 ( publicKey.getBytes ("UTF-8") );
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec ( keyBytes );
        KeyFactory keyFactory = KeyFactory.getInstance ( KEY_ALGORITHM );
        Key publicK = keyFactory.generatePublic ( x509KeySpec );
        Cipher cipher = Cipher.getInstance ( keyFactory.getAlgorithm () );
        cipher.init ( Cipher.DECRYPT_MODE, publicK );
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密  
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal ( encryptedData, offSet, MAX_DECRYPT_BLOCK );
            } else {
                cache = cipher.doFinal ( encryptedData, offSet, inputLen - offSet );
            }
            out.write ( cache, 0, cache.length );
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray ();
        out.close ();
        return decryptedData;
    }

    /** */
    /**
     * <p>
     * 公钥加密
     * </p>
     *
     * @param data      源数据
     * @param publicKey 公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPublicKey(byte[] data, String publicKey)
            throws Exception {
        byte[] keyBytes = Base64.decodeBase64 ( publicKey.getBytes ("UTF-8") );

        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec ( keyBytes );
        KeyFactory keyFactory = KeyFactory.getInstance ( KEY_ALGORITHM );
        Key publicK = keyFactory.generatePublic ( x509KeySpec );
        // 对数据加密  
        Cipher cipher = Cipher.getInstance ( keyFactory.getAlgorithm () );
        cipher.init ( Cipher.ENCRYPT_MODE, publicK );
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密  
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal ( data, offSet, MAX_ENCRYPT_BLOCK );
            } else {
                cache = cipher.doFinal ( data, offSet, inputLen - offSet );
            }
            out.write ( cache, 0, cache.length );
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray ();
        out.close ();
        return encryptedData;
    }

    /** */
    /**
     * <p>
     * 私钥加密
     * </p>
     *
     * @param data       源数据
     * @param privateKey 私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPrivateKey(byte[] data, String privateKey)
            throws Exception {
        byte[] keyBytes = Base64.decodeBase64 ( privateKey.getBytes ( "UTF-8" ) );

        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec ( keyBytes );
        KeyFactory keyFactory = KeyFactory.getInstance ( KEY_ALGORITHM );
        Key privateK = keyFactory.generatePrivate ( pkcs8KeySpec );
        Cipher cipher = Cipher.getInstance ( keyFactory.getAlgorithm () );
        cipher.init ( Cipher.ENCRYPT_MODE, privateK );
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密  
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal ( data, offSet, MAX_ENCRYPT_BLOCK );
            } else {
                cache = cipher.doFinal ( data, offSet, inputLen - offSet );
            }
            out.write ( cache, 0, cache.length );
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray ();
        out.close ();
        return encryptedData;
    }

    /** */
    /**
     * <p>
     * 获取私钥
     * </p>
     *
     * @param keyMap 密钥对
     * @return
     * @throws Exception
     */
    public static String getPrivateKey(Map<String, Object> keyMap)
            throws Exception {
        Key key = (Key) keyMap.get ( PRIVATE_KEY );
        return new String(Base64.encodeBase64 ( key.getEncoded () ),"UTF-8");
    }

    /** */
    /**
     * <p>
     * 获取公钥
     * </p>
     *
     * @param keyMap 密钥对
     * @return
     * @throws Exception
     */
    public static String getPublicKey(Map<String, Object> keyMap)
            throws Exception {
        Key key = (Key) keyMap.get ( PUBLIC_KEY );
        return new String(Base64.encodeBase64 ( key.getEncoded () ),"UTF-8");
    }

}  

