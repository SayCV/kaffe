/* gnu.classpath.tools.doclets.htmldoclet.HtmlPage
   Copyright (C) 2004 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA. */

package gnu.classpath.tools.doclets.htmldoclet;

import gnu.classpath.tools.IOToolkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import com.sun.javadoc.Tag;

/**
 *  Allows outputting an HTML document without having to build the
 *  document tree in-memory.
 */
public class HtmlPage 
{
   private PrintWriter out;
   private String pathToRoot;
   private String docType;

   public static final String DOCTYPE_FRAMESET = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">";
   public static final String DOCTYPE_STRICT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";

   public HtmlPage(File file, String pathToRoot, String encoding)
      throws IOException
   {
      this(file, pathToRoot, encoding, DOCTYPE_STRICT);
   }

   public HtmlPage(File file, String pathToRoot, String encoding, String docType)
      throws IOException
   {
      OutputStream fileOut = new FileOutputStream(file);
      Writer writer;
      if (null != encoding) {
         writer = new OutputStreamWriter(fileOut,
                                         encoding);
      }
      else {
         writer = new OutputStreamWriter(fileOut);
      }
      this.out = new PrintWriter(new BufferedWriter(writer));
      this.pathToRoot = pathToRoot;
      this.docType = docType;
   }

   public void beginElement(String elementName)
   {
      print('<');
      print(elementName);
      print('>');
   }

   public void beginElement(String elementName, String attributeName, String attributeValue)
   {
      print('<');
      print(elementName);
      print(' ');
      print(attributeName);
      print('=');
      print('\"');
      print(attributeValue);
      print('\"');
      print('>');
   }

   public void beginElement(String elementName, String[] attributeNames, String[] attributeValues)
   {
      print('<');
      print(elementName);
      for (int i=0; i<attributeNames.length; ++i) {
         if (null != attributeValues[i]) {
            print(' ');
            print(attributeNames[i]);
            print('=');
            print('\"');
            print(attributeValues[i]);
            print('\"');
         }
      }
      print('>');
   }

   public void beginElement(String elementName, String attributeName, String attributeValue, String[] attributeNames, String[] attributeValues)
   {
      print('<');
      print(elementName);
      print(' ');
      print(attributeName);
      print('=');
      print('\"');
      print(attributeValue);
      print('\"');
      if (null != attributeNames) {
         for (int i=0; i<attributeNames.length; ++i) {
            if (null != attributeValues[i]) {
               print(' ');
               print(attributeNames[i]);
               print('=');
               print('\"');
               print(attributeValues[i]);
               print('\"');
            }
         }
      }
      print('>');
   }

   public void atomicElement(String elementName)
   {
      print('<');
      print(elementName);
      print("/>");
   }

   public void atomicElement(String elementName, String attributeName, String attributeValue)
   {
      print('<');
      print(elementName);
      print(' ');
      print(attributeName);
      print('=');
      print('\"');
      print(attributeValue);
      print('\"');
      print("/>");
   }

   public void atomicElement(String elementName, String[] attributeNames, String[] attributeValues)
   {
      print('<');
      print(elementName);
      for (int i=0; i<attributeNames.length; ++i) {
         if (null != attributeValues[i]) {
            print(' ');
            print(attributeNames[i]);
            print('=');
            print('\"');
            print(attributeValues[i]);
            print('\"');
         }
      }
      print("/>");
   }


   public void endElement(String elementName)
   {
      print("</");
      print(elementName);
      print('>');
   }

   
   public void beginDiv(CssClass cssClass)
   {
      String[] divAttributeNames = cssClass.getAttributeNames();
      String[] divAttributeValues = cssClass.getAttributeValues();
      if (null == divAttributeNames) {
         divAttributeNames = new String[0];
      }
      if (null == divAttributeValues) {
         divAttributeValues = new String[0];
      }

      String[] attributeNames = new String[1 + divAttributeNames.length];
      String[] attributeValues = new String[1 + divAttributeValues.length];
      
      System.arraycopy(divAttributeNames, 0, attributeNames, 1, divAttributeNames.length);
      System.arraycopy(divAttributeValues, 0, attributeValues, 1, divAttributeNames.length);

      attributeNames[0] = "class";
      attributeValues[0] = cssClass.getName();

      beginElement(cssClass.getDivElementName(), attributeNames, attributeValues);
      if (null != cssClass.getInnerElementName()) {
         beginElement(cssClass.getInnerElementName());
      }
   }

   public void endDiv(CssClass cssClass)
   {
      if (null != cssClass.getInnerElementName()) {
         endElement(cssClass.getInnerElementName());
      }
      endElement(cssClass.getDivElementName());
   }

   public void beginSpan(CssClass cssClass)
   {
      beginElement(cssClass.getSpanElementName(), "class", cssClass.getName());
   }

   public void endSpan(CssClass cssClass)
   {
      endElement(cssClass.getSpanElementName());
   }

   public void hr()
   {
      atomicElement("hr");
   }

   public void br()
   {
      atomicElement("br");
   }
   
   public void print(String text)
   {
      out.print(text);
   }

   public void print(char c)
   {
      out.print(c);
   }

   public void div(CssClass cssClass, String contents)
   {
      beginDiv(cssClass);
      print(contents);
      endDiv(cssClass);
   }

   public void span(CssClass cssClass, String contents)
   {
      beginSpan(cssClass);
      print(contents);
      endSpan(cssClass);
   }

   public void beginPage(String title, String charset)
   {
      print("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\n");
      print(docType);
      print("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">");
      beginElement("head");
      beginElement("title");
      print(title);
      endElement("title");
      beginElement("script", 
                    new String[] { "src", "type" },
                    new String[] { pathToRoot + "/resources/gjdoc.js", "text/javascript" });
      print("<!-- this comment required for konqueror 3.2.2 -->");
      endElement("script");
      atomicElement("meta", 
                    new String[] { "http-equiv", "content" },
                    new String[] { "Content-Type", "text/html; charset=" + charset });
      atomicElement("meta", 
                    new String[] { "name", "content" },
                    new String[] { "generator", "GNU Gjdoc Standard Doclet" });
      atomicElement("link", 
                    new String[] { "rel", "type", "href", "title" },
                    new String[] { "stylesheet", "text/css", 
                                   pathToRoot + "/resources/gjdochtml-clean-layout.css",
                                   "GNU Clean" });
      atomicElement("link", 
                    new String[] { "rel", "type", "href", "title" },
                    new String[] { "stylesheet", "text/css", 
                                   pathToRoot + "/resources/gjdochtml-clean-color1.css",
                                   "GNU Clean" });
      endElement("head");
   }

   public void endPage()
   {
      endElement("html");
   }

   public void close()
   {
      out.close();
   }

   public void beginTable(CssClass cssClass)
   {
      beginElement("table", "class", cssClass.getName());
   }

   public void beginTable(CssClass cssClass, String[] attributeNames, String[] attributeValues)
   {
      beginElement("table", "class", cssClass.getName(), attributeNames, attributeValues);
   }

   public void beginRow()
   {
      beginElement("tr");
   }

   public void beginRow(CssClass cssClass)
   {
      beginElement("tr", "class", cssClass.getName(), cssClass.getAttributeNames(), cssClass.getAttributeValues());
   }

   public void beginRow(String attribute, String value)
   {
      beginElement("tr", attribute, value);
   }

   public void beginCell()
   {
      beginElement("td");
   }

   public void beginCell(String attribute, String value)
   {
      beginElement("td", attribute, value);
   }

   public void beginCell(CssClass cssClass)
   {
      beginElement("td", "class", cssClass.getName(), cssClass.getAttributeNames(), cssClass.getAttributeValues());
   }

   public void endCell()
   {
      endElement("td");
   }

   public void cell(CssClass cssClass, String contents)
   {
      beginCell(cssClass);
      print(contents);
      endCell();
   }

   public void endRow()
   {
      endElement("tr");
   }

   public void rowDiv(CssClass cssClass, String contents)
   {
      beginRow(cssClass);
      beginCell("colspan", "2");
      beginDiv(cssClass);
      print(contents);
      endDiv(cssClass);
      endCell();
      endRow();
   }

   public void endTable()
   {
      endElement("table");
   }

   public void beginAnchor(String href)
   {
      beginElement("a", "href", href);
   }

   public void beginAnchor(String href, String title)
   {
      beginElement("a", 
                   new String[] { "href", "title" },
                   new String[] { href, title });
   }

   public void beginAnchor(String href, String title, String target)
   {
      beginElement("a", 
                   new String[] { "href", "title", "target" },
                   new String[] { href, title, target });
   }

   public void endAnchor()
   {
      endElement("a");
   }

   public void anchor(String href, String label)
   {
      beginAnchor(href);
      print(label);
      endAnchor();
   }

   public void anchorName(String name)
   {
      atomicElement("a", "name", name);
   }

   public String getPathToRoot()
   {
      return pathToRoot;
   }

   public void beginBody(CssClass cssClass)
   {
      beginBody(cssClass, true);
   }

   public void beginBody(CssClass cssClass, boolean setTitle)
   {
      if (setTitle) {
         beginElement("body", 
                      new String[] { "class", "onload" },
                      new String[] { cssClass.getName(), "top.contentPageLoaded(document.title)" }
                      );
      }
      else {
         beginElement("body",
                      new String[] { "class", "onload" },
                      new String[] { cssClass.getName(), "top.contentPageLoaded()" }
                      );
      }
   }

   public void endBody()
   {
      endElement("body");
   }

   public void insert(Reader in)
      throws IOException
   {
      IOToolkit.copyStream(in, out);
   }

   public String createHrefString(String url, String content)
   {
      return createHrefString(url, content, null);
   }

   public String createHrefString(String url, String content, String title)
   {
      StringBuffer result = new StringBuffer();
      result.append("<a href=\"");
      result.append(url);
      result.append("\"");
      if (null != title) {
         result.append(" title=\"");
         result.append(title);
         result.append("\"");
      }
      result.append(">");
      result.append(content);
      result.append("</a>");
      return result.toString();
   }
}