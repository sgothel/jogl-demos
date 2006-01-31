
package demos.misc;

/**
 * VersionInfo.java <BR>
 * author: Travis Bryson <P>
 *
 * This program returns the version and implementation information for the Java 
 * Bindings for OpenGL (R) implementation found in the CLASSPATH.  This information
 * is also found in the manifest for jogl.jar, and this program uses the 
 * java.lang.Package class to retrieve it programatically.  
**/

public class VersionInfo {
    public VersionInfo() {
	ClassLoader classLoader = getClass().getClassLoader();
	pkgInfo(classLoader, "javax.media.opengl", "GL");
    }

    static void pkgInfo(ClassLoader classLoader,
			String pkgName,
			String className) {

	try {
	    classLoader.loadClass(pkgName + "." + className);

	    Package p = Package.getPackage(pkgName);
	    if (p == null) {
		System.out.println("WARNING: Package.getPackage(" +
				   pkgName +
				   ") is null");
	    }
	    else {
		System.out.println(p);
		System.out.println("Specification Title = " +
				   p.getSpecificationTitle());
		System.out.println("Specification Vendor = " +
				   p.getSpecificationVendor());
		System.out.println("Specification Version = " +
				   p.getSpecificationVersion());

		System.out.println("Implementation Vendor = " +
				   p.getImplementationVendor());
		System.out.println("Implementation Version = " +
				   p.getImplementationVersion());
	    }
	}
	catch (ClassNotFoundException e) {
	    System.out.println("Unable to load " + pkgName);
	}

	System.out.println();
    }

    public static void main(String[] args) {
	new VersionInfo();
    }
}
