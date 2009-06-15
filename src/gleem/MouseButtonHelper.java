/*
 * gleem -- OpenGL Extremely Easy-To-Use Manipulators.
 * Copyright (C) 1998-2003 Kenneth B. Russell (kbrussel@alum.mit.edu)
 *
 * Copying, distribution and use of this software in source and binary
 * forms, with or without modification, is permitted provided that the
 * following conditions are met:
 *
 * Distributions of source code must reproduce the copyright notice,
 * this list of conditions and the following disclaimer in the source
 * code header files; and Distributions of binary code must reproduce
 * the copyright notice, this list of conditions and the following
 * disclaimer in the documentation, Read me file, license file and/or
 * other materials provided with the software distribution.
 *
 * The names of Sun Microsystems, Inc. ("Sun") and/or the copyright
 * holder may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS," WITHOUT A WARRANTY OF ANY
 * KIND. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INTERFERENCE, ACCURACY OF
 * INFORMATIONAL CONTENT OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. THE
 * COPYRIGHT HOLDER, SUN AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL THE
 * COPYRIGHT HOLDER, SUN OR SUN'S LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES. YOU ACKNOWLEDGE THAT THIS SOFTWARE IS NOT
 * DESIGNED, LICENSED OR INTENDED FOR USE IN THE DESIGN, CONSTRUCTION,
 * OPERATION OR MAINTENANCE OF ANY NUCLEAR FACILITY. THE COPYRIGHT
 * HOLDER, SUN AND SUN'S LICENSORS DISCLAIM ANY EXPRESS OR IMPLIED
 * WARRANTY OF FITNESS FOR SUCH USES.
 */

package gleem;

import java.security.*;

/** Helper class for figuring out how many mouse buttons are
    available. (Does not seem to be a way of figuring this out with
    the AWT.) */

public class MouseButtonHelper {
  /** Returns the number of buttons on the mouse device. This is only
      a guess and the implementation may need to be extended to
      support other operating systems (in particular, Mac OS). */
  public static int numMouseButtons() {
    String osName = null;
    if (System.getSecurityManager() != null) {
      osName = (String)
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              return System.getProperty("os.name");
            }
          });
    } else {
      osName = System.getProperty("os.name");
    }
    return mouseButtonsForOS(osName);
  }

  private static int mouseButtonsForOS(String osName) {
    if (osName.startsWith("Windows")) {
      return 2;
    } else {
      // Assume X11 and a three-button mouse
      return 3;
    }
  }
}
