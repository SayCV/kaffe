/*
 * ExecJarName.java
 * Execute a Jar file with a given named starting class.
 *
 * Copyright (c) 1998
 *      Transvirtual Technologies, Inc.  All rights reserved.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file.
 */

package kaffe.jar;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.Character;
import java.lang.String;
import java.lang.reflect.Method;
import java.util.jar.*;
import kaffe.management.Classpath;

public class ExecJarName {

public static JarFile getJar(String path) {
	JarFile jar = null;
	try {
		jar = new JarFile(path);
	}
	catch (IOException e) {
		System.err.println("Can't access JAR file ``"
		    + path + "'': " + e);
		System.exit(1);
	}
	return jar;
}

public static void main(String[] args)
{
	/* Check JAR file OK */
	getJar(args[0]);

	/* Add JAR to classpath */
	Classpath.add(args[0]);

	/* Get command to call */
	String command = args[1];

	/* Build new argument array */
	String[] nargs = new String[args.length-2];
	for (int i = 2; i < args.length; i++) {
		nargs[i-2] = args[i];
	}

	/* Load in class */
	try {
		Class commandClass = Class.forName(command);

		/* Build main(String[])V */
		Class[] params = new Class[1];
		params[0] = (new String[0]).getClass();

		/* Build the invoke arguments */
		Object[] iargs = new Object[1];
		iargs[0] = (Object)nargs;

		/* Get method and invoke */
		Method meth = commandClass.getDeclaredMethod("main", params);
		meth.invoke(null, iargs);
	}
	catch (Exception misc) {
		misc.printStackTrace();
		System.exit(1);
	}
}

}
