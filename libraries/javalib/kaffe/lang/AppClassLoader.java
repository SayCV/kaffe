/*
 * Java core library component.
 *
 * Copyright (c) 1997, 1998
 *      Transvirtual Technologies, Inc.  All rights reserved.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file.
 */

package kaffe.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * ClassLoader used to load application classes from the CLASSPATH.
 */
public class AppClassLoader extends URLClassLoader {

	private static final AppClassLoader SINGLETON =
		new AppClassLoader();
	
	/**
	 * One source of classes.
	 */
	private abstract class Source {
		Source next;
                CodeSource codeSource;

		abstract Class findClass (String name, String fileName);

		abstract void findResources (Vector v, String name);
	}

	/**
	 * A source which is a directory.
 	 */
	private final class DirSource extends Source {
		File dir;
                
		DirSource (File f) throws IOException {
			dir = f.getCanonicalFile ();
		}

		Class findClass (String name, String fileName) {
			File file = new File (dir, fileName);

			if (!file.exists ()) {
				return null;
			}

			try {
				int length = (int)file.length();
				FileInputStream in = new FileInputStream (file);
				byte[] buf = new byte[length];
				for (int j=0; j<length; j+=in.read (buf, j, length-j));

				if (codeSource == null) {
					try {
						codeSource = new CodeSource (dir.toURL(), new Certificate[0]);
					} catch (MalformedURLException _) {}
				}

				Class ret = defineClass (name, buf, 0, length, codeSource);

				String pkgName = PackageHelper.getPackageName(ret);
				if (getPackage(pkgName) == null) {
					definePackage(pkgName, null, null, null, null, null, null, null);
                                }

				return ret;
			} catch (Exception _) {}

			return null;
		}
		
		void findResources (Vector v, String name) {
			File file = new File (dir, name);

			if (file.exists ()) {
				try {
					v.add (file.toURL());
				} catch (MalformedURLException _) {}
			}
		}

		public String toString() {
			return "DirSource[dir=" + this.dir + "]";
		}
	}

	/**
	 * A source which is a jar file.
	 */
	private final class JarSource extends Source {
		JarFile jar;
		String urlPrefix;
                File file;

		public JarSource (File file) throws IOException {
			File canonicalFile = file.getCanonicalFile ();
			this.jar = new JarFile (canonicalFile); 
                        this.urlPrefix = "jar:file:"+canonicalFile.getPath().replace(File.separatorChar, '/') + "!/";
			this.file = canonicalFile;
		}
		
		Class findClass (String name, String fileName) {
			ZipEntry classFile = jar.getEntry(fileName);

			if (classFile==null) {
				return null;
			}

               		int length = (int)classFile.getSize();
               		byte[] buf = new byte[length];
             		try {
				InputStream in = jar.getInputStream(classFile);
				for (int j=0;j<length;j+=in.read(buf, j, length-j));
					
				if (codeSource == null) {
					try {
						codeSource = new CodeSource (file.toURL(), new Certificate[0]);
					} catch (MalformedURLException _) {}
       				}
		
                                Class ret = defineClass (name, buf, 0, length, codeSource);

				String pkgName = PackageHelper.getPackageName(ret);
				if (getPackage(pkgName) == null) {
					Manifest mf = jar.getManifest();

					if (mf == null) {
						definePackage(pkgName, null, null, null, null, null, null, null);
					} else {
						Attributes attrs = jar.getManifest().getAttributes (pkgName);

						if (attrs == null) {
							attrs = jar.getManifest().getMainAttributes();
						}
                       
			 			definePackage(pkgName,  
                                			attrs.getValue(Attributes.Name.SPECIFICATION_TITLE),
	                                		attrs.getValue(Attributes.Name.SPECIFICATION_VERSION),
        	                        		attrs.getValue(Attributes.Name.SPECIFICATION_VENDOR),
                	                		attrs.getValue(Attributes.Name.IMPLEMENTATION_TITLE),
                        	        		attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION),
                                			attrs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR),
                                			null);
					}
				}

				return ret;
			} catch (IOException _) {}
			return null;
		}

		void findResources (Vector v, String name) {
			ZipEntry resourceFile = jar.getEntry (name);

			if (resourceFile!=null && !resourceFile.isDirectory()) {
				try {
					v.addElement(new URL(urlPrefix + resourceFile.getName()));
				} catch (MalformedURLException _) {_.printStackTrace();}
			}
		}
		
		public String toString() {
			return "JarSource[file=" + this.file + "]";
		}
	}

/* linked list of Sources as extracted from classpath */
private Source sources;
	
public static ClassLoader getSingleton() {
	return SINGLETON;
}

private String listPath() {
	String retval = "";
	Source curr;

	curr = this.sources;
	while( curr != null )
	{
		retval += curr + File.pathSeparator;
		curr = curr.next;
	}
	return retval;
}

public void addSource(String path) {
	File f = new File (path);
	
	if (!f.exists()) {
		return;
	}
	
	Source s = null;
	
	try {
		if (f.isDirectory()) {
			s = new DirSource (f.getAbsoluteFile ());
		} else {
			s = new JarSource (f.getAbsoluteFile ());
		}

		if (this.sources == null) {
			this.sources = s;
		} else {
			Source last;

			for( last = this.sources;
			     last.next != null;
			     last = last.next );
			last.next = s;
		}
	} catch (Exception _) { }
}
	
private AppClassLoader() {
	super(new URL[0]);

	StringTokenizer tok = new StringTokenizer (System.getProperty("java.class.path"), File.pathSeparator);

        while (tok.hasMoreTokens ())
        {
		this.addSource(tok.nextToken().trim());
 	}    	   
}

/*
 * Search through the CLASSPATH directories and ZIP files to find
 * the named resource (which may appear more than once). Make sure
 * it really exists in each place before adding it.
 */
public Enumeration findResources(String name) throws IOException {
	Vector v = new Vector();

	if (name.charAt(0) == '/') {
		name = name.substring (1);
	}	
	
	for (Source i=this.sources; i!=null; i=i.next) {
		i.findResources (v, name);
	}
	return v.elements();
}

public URL findResource (String name) {
	Vector v = new Vector();

	if (name.charAt(0) == '/') {
		name = name.substring (1);
	}

	for (Source i=this.sources; i!=null && v.size()==0; i=i.next) {
		i.findResources (v, name);
	}

	if (v.size()>0) {
		return (URL)v.elementAt(0);
	} else {
		return null;
	}
}

protected Class findClass(String name) throws ClassNotFoundException {
	Class ret = null;
	
	String fileName = name.replace ('.', '/') + ".class";

	for (Source i=this.sources; i!=null && ret==null; i=i.next) {
		ret = i.findClass (name, fileName);
	}

	// throw an error, if nobody is able to find that class
        if (ret == null) {
		throw new ClassNotFoundException (name);
	} 

	return ret;
}

}
