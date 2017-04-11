package com.cloud.cloudapi.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;




/** 
* @author  wangw
* @create  2016年9月8日 上午9:59:56 
* 
*/
public class MD5Helper {
	
	/**
	 * 32位MD5加密算法
	 * @param plainText
	 * @return
	 */
	public static String encode(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();// 32位的加密

        } catch (NoSuchAlgorithmException e) {
        	
        }
        return null;
    }
    

}
