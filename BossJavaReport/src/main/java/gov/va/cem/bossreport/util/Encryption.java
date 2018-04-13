/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.va.cem.bossreport.util;
/**
 * General utilities for encryption and decryption.
 * Note: this is using a constant key
 */

import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
//import javax.crypto.KeyGenerator;
//import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.InvalidKeyException;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

/*
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
*/

public class Encryption {

    private static String algorithm = "DES"; //"DESede";
    private static String cipherMode = "CFB8";
    private static String paddingScheme = "PKCS5Padding";
    private static SecretKeySpec keySpec = null;
    private static Cipher cipher = null;


    private static String digits = "0123456789abcdef";

    //private static final char[] kDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
    //    'b', 'c', 'd', 'e', 'f' };

    //private static final String hexKey = "3b0e15bffe8fc2a4";
    private static final byte[] desKey = 
        //below after the 0x are hex. the ones with (byte) are negative
        new byte[] { 0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, 
                     (byte)0xcd, (byte)0xef };
        //equiv= 1 35 69 103 -119 -85 -51 -17 if you'll print the desKey using:
        //for(int i=0;i<desKey.length;i++) {
        //System.out.print(desKey[i] + " ");
        //}
    private static final byte[] IV = 
        //below after the 0x are hex. the ones with (byte) are negative
        new byte[] { (byte)0xb8, (byte)0xcd, (byte)0xa1, (byte)0xf5, 0x28, 
                     0x78, (byte)0xab, (byte)0x27 };
        //equiv = -72 -51 -95 -11 40 120 -85 39

    /**
     * Return the passed in string as a decrypted string in hex representation
     *
     * @param input the string to be encrypted.
     * @return a hex representation of data but it still a string
     */
    public static String /*byte[]*/encrypt(String input) {
        input = 
                "________" + input; //added 8 chars as the correct decrypted string only started at 9th char
        byte[] outputBytes = null;
        setUpKey();
        try {
            //test
            //System.out.println("encrypting, here is key in bytes " + keySpec.getEncoded());

            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] inputBytes = input.getBytes();
            outputBytes = cipher.doFinal(inputBytes);
        } catch (InvalidKeyException ike) {
            //Highly unlikely that a SecretKey from a KeyGenerator engine would get here
            //However, we never assume!
            //Handle this!
            ike.printStackTrace();
        } catch (IllegalBlockSizeException ibse) {
            ibse.printStackTrace();
        } catch (BadPaddingException bpe) {
            bpe.printStackTrace();
        }
        return toHex(outputBytes);
        //String outString = new String(inputBytes);
    }


    /**
     * Return the passed in encrypted string as plain text string
     *
     * @param hexString the string to be encrypted.
     * @return a plaintext 
     */
    public static
    //public static String decrypt(byte[] inputBytes)
    String decrypt(String hexString) {
        byte[] inputBytes = hexToBytes(hexString);

        setUpKey();

        String recovered = "";

        try {
            IvParameterSpec iv = new IvParameterSpec(IV);
            //System.out.println("decrypting,  here is key in bytes " + keySpec.getEncoded());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
        } catch (InvalidKeyException ike) {
            ike.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            //Handle this
            e.printStackTrace();
        }

        try {
            byte[] recoveredBytes = cipher.doFinal(inputBytes);
            recovered = new String(recoveredBytes);
        } catch (IllegalBlockSizeException ibse) {
            ibse.printStackTrace();
        } catch (BadPaddingException bpe) {
            bpe.printStackTrace();
        }
        return recovered.substring(8);
    }


    public static byte[] hexToBytes(char[] hex) {
        int length = hex.length / 2;
        byte[] raw = new byte[length];
        for (int i = 0; i < length; i++) {
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            if (value > 127)
                value -= 256;
            raw[i] = (byte)value;
        }
        return raw;
    }

    public static byte[] hexToBytes(String hex) {
        return hexToBytes(hex.toCharArray());
    }


    /**
     * Return length many bytes of the passed in byte array as a hex string.
     *
     * @param data the bytes to be converted.
     * @param length the number of bytes in the data block to be converted.
     * @return a hex representation of length bytes of data.
     */
    public static


    String toHex(byte[] data, int length) {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i != length; i++) {
            int v = data[i] & 0xff;

            buf.append(digits.charAt(v >> 4));
            buf.append(digits.charAt(v & 0xf));
        }

        return buf.toString();
    }

    /**
     * Return the passed in byte array as a hex string.
     *
     * @param data the bytes to be converted.
     * @return a hex representation of data.
     */
    public static String toHex(byte[] data) {
        return toHex(data, data.length);
    }

    /**
     * Convert a byte array of 8 bit characters into a String.
     *
     * @param bytes the array containing the characters
     * @param length the number of bytes to process
     * @return a String representation of bytes
     */
    public static String toString(byte[] bytes, int length) {
        char[] chars = new char[length];

        for (int i = 0; i != chars.length; i++) {
            chars[i] = (char)(bytes[i] & 0xff);
        }

        return new String(chars);
    }

    /**
     * Convert a byte array of 8 bit characters into a String.
     *
     * @param bytes the array containing the characters
     * @return a String representation of bytes
     */
    public static String toString(byte[] bytes) {
        return toString(bytes, bytes.length);
    }


    private static void setUpKey() { //throws Exception {
        /*
    		byte[] desKey = null; //hexToBytes(hexKey);

			//Get an instance of a KeyGenerator engine for DES encryption
			try
			{
					KeyGenerator kg = KeyGenerator.getInstance(algorithm);
					SecretKey key = kg.generateKey();

				//Translate our key into its encoded byte array
					desKey = key.getEncoded();

		try {
			FileInputStream fisKey = new FileInputStream(new File("C:/denm/Java/Encryption_JCA/forGAR/mykey.txt"));

			desKey = new byte[fisKey.available()];
			fisKey.read(desKey);
			fisKey.close();
		} catch (IOException keyIoe)
		{
			//Can't read in the key?
			System.err.println("Can't read in mykey.txt.");
		}

			System.out.println("Key Bytes Retrieved:");
			for(int i=0;i<desKey.length;i++)
			{
				System.out.print(desKey[i] + " ");
			}
			System.out.println("");
*/
        //Turn the key into a key specification
        keySpec = new SecretKeySpec(desKey, algorithm);
        /*
    System.out.println("here is the key in bytes " + keySpec.getEncoded());
    System.out.println("   key using Utils.toHex " + Utils.toHex(keySpec.getEncoded()) +"\n");
    System.out.println("           key as String " + toString(keySpec.getEncoded()));
    System.out.println("key as using (byteArray) " + new String(keySpec.getEncoded()));
*/
        /*
		try {
					FileOutputStream keyFos = new FileOutputStream(new File("C:/denm/Java/Encryption_JCA/forGAR/mykey.txt"));
					keyFos.write(keySpec.getEncoded());
					keyFos.close();
				} catch (IOException ioe)
				{
					//Handle this!
					ioe.printStackTrace();
				}
*/

        /*
				byte[] byteKeySpec = keySpec.getEncoded(); //save this later as hex so we can use for decryption)
    System.out.println("here is key in bytes " + byteKeySpec);
    System.out.println("key using Utils.toHex " + Utils.toHex(byteKeySpec));
    //System.out.println("key as String using toString" + toString(byteKeySpec));
    //System.out.println("key as String using new String(byteArray) " + new String(byteKeySpec));
*/

        try {

            cipher = 
                    Cipher.getInstance(algorithm + "/" + cipherMode + "/" + paddingScheme);

        } catch (NoSuchPaddingException nspe) {
            nspe.printStackTrace();
        } catch (NoSuchAlgorithmException nsae) {
            //DES isn't available; must not have SunJCE in your java.security list!
            //Handle this!
            nsae.printStackTrace();
        }

        /*
	//use this catch when deriving the key
			} catch (NoSuchAlgorithmException nsae)	{
				//DES isn't available; must not have SunJCE in your java.security list!
				//Handle this!
				nsae.printStackTrace();
			}
*/
    }


}
