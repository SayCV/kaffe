package kaffe.io;

import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.util.Hashtable;

/*
 * Java core library component.
 *
 * Copyright (c) 1997, 1998
 *      Transvirtual Technologies, Inc.  All rights reserved.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file.
 */
abstract public class CharToByteConverter
{
	protected char[] buf;
	protected int blen;
	private static String encodingRoot;
	private static String encodingDefault;
	private static Hashtable cache = new Hashtable();

static {
	// see explanation in ByteToCharConverter
	encodingRoot = "kaffe.io";
	encodingDefault = System.getProperty("file.encoding");
}

public CharToByteConverter() {
}

public void carry ( char[] from, int fpos, int flen ) {
	int n;
	int m = blen + flen;

	if ( (from == buf) && (fpos == 0) ) // avoid recursive carry by flush()
		return;

	if ( buf == null ){
		n = (flen < 128) ? 128 : flen;
		buf = new char[n];
	}
	else {
		if ( m > buf.length ) {
			for ( n=buf.length*2; n<m; n *= 2);
			char[] newBuf = new char[n];
			System.arraycopy( buf, 0, newBuf, 0, blen);
			buf = newBuf;
		}
	}

	System.arraycopy( from, fpos, buf, blen, flen);
	blen = m;
}

abstract public int convert(char[] from, int fpos, int flen, byte[] to, int tpos, int tlen);

public int flush ( byte[] to, int tpos, int tlen ) {
	if ( blen == 0 ){
		return 0;
	}
	else {
		int oblen = blen;
		blen = 0;
		return (convert( buf, 0, oblen, to, tpos, tlen));
	}
}

public static CharToByteConverter getConverter(String enc) throws UnsupportedEncodingException
{
	CharToByteConverter conv;

	conv = (CharToByteConverter)cache.get(enc);
	if (conv != null) {
		return (conv);
	}

	String realenc = ConverterAlias.alias(enc);
	try {
		conv = (CharToByteConverter)Class.forName(encodingRoot + ".CharToByte" + realenc).newInstance();
		cache.put(enc, conv);
		return (conv);
	}
	catch (ClassNotFoundException _) {
		throw new UnsupportedEncodingException(enc);
	}
	catch (ClassCastException _) {
		throw new UnsupportedEncodingException(enc);
	}
	catch (InstantiationException _) {
		throw new UnsupportedEncodingException(enc);
	}
	catch (IllegalAccessException _) {
		throw new UnsupportedEncodingException(enc);
	}
}

public static CharToByteConverter getDefault() {
	String enc = encodingDefault;
	if (ClassLoader.getSystemResourceAsStream(encodingRoot + ".CharToByte" + enc) == null) {
		enc = "Default";
	}
	try {
		return (getConverter(enc));
	}
	catch (UnsupportedEncodingException _) {
		return (null);
	}
}

abstract public int getNumberOfBytes ( char[] from, int fpos, int flen );
}
