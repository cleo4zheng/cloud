package com.cloud.cloudapi.util;

import java.io.UnsupportedEncodingException;

public class StringHelper {
	
	
	public static boolean isNullOrEmpty(String str){
		
		if(str==null||"".equals(str)) return true;
		return false;
			
	}
	
	
	public static String objectToString(Object object){
		if (object == null){
			return "";
		}else{
			return object.toString();
		}
			
		
	}
	
	
	/**
	    * Encode a String with Numeric Character Refernces.
	    * <p>
	    * Formats each character < 0x20 or > 0x7f to &#nnnn; where nnnn is the char value as int.
	    * <p>
	    *  
	    * @param str The raw String
	    * @return The encoded String
	    */
	public static String string2Ncr(String str){
		if(null == str)
			return null;
		char[] ch = str.toCharArray();
	     StringBuffer sb = new StringBuffer();
	     for ( int i = 0 ; i < ch.length ; i++ ) {
	      if ( ch[i] < 0x20 || ch[i] > 0x7f )
	        sb.append("&#").append((int) ch[i]).append(";");
	      else
	        sb.append(ch[i]);
	     }
	     return sb.toString();
	}
	
	
	/**
	   * Decodes a String with Numeric Character References.
	   * <p>
	   * 
	   * @param str A NCR encoded String
	   * @param unknownCh, A character that is used if nnnn of &#nnnn; is not a int.
	   * 
	   * @return The decoded String.
	   */
	public static String ncr2String(String str) {
		if(null == str)
			return str;
        StringBuffer sb = new StringBuffer();
        int i1=0;
        int i2=0;

        while(i2<str.length()) {
           i1 = str.indexOf("&#",i2);
           if (i1 == -1 ) {
                sb.append(str.substring(i2));
                break ;
           }
           sb.append(str.substring(i2, i1));
           i2 = str.indexOf(";", i1);
           if (i2 == -1 ) {
                sb.append(str.substring(i1));
                break ;
           }

           String tok = str.substring(i1+2, i2);
            try {
                 int radix = 10 ;
                 if (tok.charAt(0) == 'x' || tok.charAt(0) == 'X') {
                    radix = 16 ;
                    tok = tok.substring(1);
                 }
                 sb.append((char) Integer.parseInt(tok, radix));
            } catch (NumberFormatException exp) {
                 sb.append("");
            }
            i2++ ;
        }
        return sb.toString();
	}
	public static void main(String[] args) throws UnsupportedEncodingException{
		
		String nanjing = "你好";
		System.out.println(string2Ncr(nanjing) );
		System.out.println(ncr2String("&#20320;xxxx&#22909;"));
	}

}
