/* gnu.classpath.tools.doclets.htmldoclet.HtmlDoclet
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

import gnu.classpath.tools.doclets.AbstractDoclet;
import gnu.classpath.tools.doclets.DocletConfigurationException;
import gnu.classpath.tools.doclets.DocletOption;
import gnu.classpath.tools.doclets.DocletOptionFile;
import gnu.classpath.tools.doclets.DocletOptionFlag;
import gnu.classpath.tools.doclets.DocletOptionString;
import gnu.classpath.tools.doclets.PackageGroup;
import gnu.classpath.tools.doclets.TagletPrinter;
import gnu.classpath.tools.doclets.InlineTagRenderer;

import gnu.classpath.tools.doclets.xmldoclet.HtmlRepairer;

import gnu.classpath.tools.taglets.GnuExtendedTaglet;
import gnu.classpath.tools.taglets.TagletContext;

import gnu.classpath.tools.java2xhtml.Java2xhtml;

import gnu.classpath.tools.StringToolkit;

import com.sun.javadoc.*;
import com.sun.tools.doclets.Taglet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.net.MalformedURLException;

import java.nio.charset.Charset;

import java.text.MessageFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class HtmlDoclet
   extends AbstractDoclet
   implements InlineTagRenderer
{
   private static String filenameExtension = ".html";

   /**
    *  Contains ExternalDocSet.
    */
   private List externalDocSets = new LinkedList();

   /**
    *  Contains String->ExternalDocSet.
    */
   private Map packageNameToDocSet = new HashMap();

   private void printNavBar(HtmlPage output, String currentPage, ClassDoc currentClass)
   {
         output.beginDiv(CssClass.NAVBAR_TOP);

         boolean overviewLevel
            = ("overview".equals(currentPage)
               || "full-tree".equals(currentPage)
               || "index".equals(currentPage)
               || "serialized".equals(currentPage)
               || "deprecated".equals(currentPage)
               );

         if (!isSinglePackage()) {
            if ("overview".equals(currentPage)) {
               output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
               output.print("Overview");
               output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
            }
            else {
               output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
               output.beginAnchor(output.getPathToRoot() + "/index-noframes" + filenameExtension);
               output.print("Overview");
               output.endAnchor();
               output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
            }
            
            output.print(" ");
         }

         if (!overviewLevel) {
            if ("package".equals(currentPage)) {
               output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
               output.print("Package");
               output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
            }
            else {
               output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
               output.beginAnchor("package-summary" + filenameExtension);
               output.print("Package");
               output.endAnchor();
               output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
            }
         }
         else {
            output.beginSpan(CssClass.NAVBAR_ITEM_DISABLED);
            output.print("Package");
            output.endSpan(CssClass.NAVBAR_ITEM_DISABLED);
         }

         if (optionUse.getValue() || optionLinkSource.getValue()) {
            output.print(" ");

            if (null != currentClass) {
               if ("class".equals(currentPage)) {
                  output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
                  output.print("Class");
                  output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
               }
               else {
                  output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
                  output.beginAnchor(currentClass.name() + filenameExtension);
                  output.print("Class");
                  output.endAnchor();
                  output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
               }
            }
            else {
               output.beginSpan(CssClass.NAVBAR_ITEM_DISABLED);
               output.print("Class");
               output.endSpan(CssClass.NAVBAR_ITEM_DISABLED);
            }

            if (optionUse.getValue()) {
               output.print(" ");

               if (null != currentClass) {
                  output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
                  output.beginAnchor(currentClass.name() + "-uses" + filenameExtension);
                  output.print("Use");
                  output.endAnchor();
                  output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
               }
               else {
                  output.beginSpan(CssClass.NAVBAR_ITEM_DISABLED);
                  output.print("Use");
                  output.endSpan(CssClass.NAVBAR_ITEM_DISABLED);
               }
            }

            if (optionLinkSource.getValue()) {
               output.print(" ");

               if (null != currentClass) {
                  output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
                  String targetClassName = currentClass.name();
                  String targetAnchor = "";
                  if (null != currentClass.containingClass()) {
                     targetClassName = getOuterClassDoc(currentClass).name();
                     targetAnchor = "#line." + currentClass.position().line();
                  }
                  output.beginAnchor(targetClassName + "-source" + filenameExtension + targetAnchor);
                  output.print("Source");
                  output.endAnchor();
                  output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
               }
               else {
                  output.beginSpan(CssClass.NAVBAR_ITEM_DISABLED);
                  output.print("Source");
                  output.endSpan(CssClass.NAVBAR_ITEM_DISABLED);
               }
            }
         }


         if (!optionNoTree.getValue()) {
            output.print(" ");

            if ("full-tree".equals(currentPage) 
                || "package-tree".equals(currentPage)
                || (isSinglePackage() && overviewLevel)) {
               output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
               output.print("Tree");
               output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
            }
            else {
               output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
               output.beginAnchor("tree" + filenameExtension);
               output.print("Tree");
               output.endAnchor();
               output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
            }
         }

         output.print(" ");

         String indexName;
         if (optionSplitIndex.getValue()) {
            indexName = "alphaindex-1";
         }
         else {
            indexName = "alphaindex";
         }

         if ("index".equals(currentPage)) {
            output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
            output.print("Index");
            output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
         }
         else {
            output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
            output.beginAnchor(output.getPathToRoot() + "/" + indexName + filenameExtension);
            output.print("Index");
            output.endAnchor();
            output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
         }

         if (!optionNoDeprecatedList.getValue()) {
            output.print(" ");

            if ("deprecated".equals(currentPage)) {
               output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
               output.print("Deprecated");
               output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
            }
            else {
               output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
               output.beginAnchor(output.getPathToRoot() + "/deprecated" + filenameExtension);
               output.print("Deprecated");
               output.endAnchor();
               output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
            }
         }

         if (!optionNoHelp.getValue()) {
            output.print(" ");

            if ("help".equals(currentPage)) {
               output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
               output.print("Help");
               output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
            }
            else {
               output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
               output.beginAnchor(output.getPathToRoot() + "/help" + filenameExtension);
               output.print("Help");
               output.endAnchor();
               output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
            }
         }

         output.print(" ");

         if ("about".equals(currentPage)) {
            output.beginSpan(CssClass.NAVBAR_ITEM_ACTIVE);
            output.print("About");
            output.endSpan(CssClass.NAVBAR_ITEM_ACTIVE);
         }
         else {
            output.beginSpan(CssClass.NAVBAR_ITEM_ENABLED);
            output.beginAnchor(output.getPathToRoot() + "/about" + filenameExtension);
            output.print("About");
            output.endAnchor();
            output.endSpan(CssClass.NAVBAR_ITEM_ENABLED);
         }

         output.endDiv(CssClass.NAVBAR_TOP);
   }

   private void printNavBarTop(HtmlPage output, String currentPage)
   {
      printNavBarTop(output, currentPage, null);
   }

   private void printNavBarTop(HtmlPage output, String currentPage, ClassDoc currentClass)
   {
      if (!optionNoNavBar.getValue()) {
         output.beginTable(CssClass.NAVBAR_TOP);
         output.beginRow();
         output.beginCell(CssClass.NAVBAR_TOP);
         printNavBar(output, currentPage, currentClass);
         output.endCell();
         if (null != optionHeader.getValue()) {
            output.beginCell();
            output.print(replaceDocRoot(output, optionHeader.getValue()));
            output.endCell();
         }
         output.endRow();
         output.endTable();
      }
   }

   private void printNavBarBottom(HtmlPage output, String currentPage)
   {
      printNavBarBottom(output, currentPage, null);
   }

   private void printNavBarBottom(HtmlPage output, String currentPage, ClassDoc currentClass)
   {
      if ("class".equals(currentPage)) {
         String boilerplate = null;
         Tag[] boilerplateTags = getOuterClassDoc(currentClass).tags("@boilerplate");
         if (boilerplateTags.length > 0) {
            boilerplate = boilerplateTags[0].text();
         }
         if (null != boilerplate) {
            output.hr();
            output.beginDiv(CssClass.CLASS_BOILERPLATE);
            output.print(boilerplate);
            output.endDiv(CssClass.CLASS_BOILERPLATE);
            output.hr();
         }
      }

      if (!optionNoNavBar.getValue()) {
         output.beginDiv(CssClass.NAVBAR_BOTTOM_SPACER);
         output.print(" ");
         output.endDiv(CssClass.NAVBAR_BOTTOM_SPACER);
         output.beginTable(CssClass.NAVBAR_BOTTOM);
         output.beginRow();
         output.beginCell();
         printNavBar(output, currentPage, currentClass);
         output.endCell();
         if (null != optionFooter.getValue()) {
            output.beginCell();
            output.print(replaceDocRoot(output, optionFooter.getValue()));
            output.endCell();
         }
         output.endRow();
         output.endTable();
      }

      if (null != optionBottom.getValue()) {
         output.hr();
         output.print(replaceDocRoot(output, optionBottom.getValue()));
      }
   }

   private void printPackagePageClasses(HtmlPage output, ClassDoc[] classDocs, String header)
   {
      if (classDocs.length > 0) {
         output.beginDiv(CssClass.TABLE_CONTAINER);
         output.beginTable(CssClass.PACKAGE_SUMMARY, new String[] { "border", "width" }, new String[] { "1", "100%" });
         output.rowDiv(CssClass.TABLE_HEADER, header);

         for (int i=0; i<classDocs.length; ++i) {
            ClassDoc classDoc = classDocs[i];
            if (classDoc.isIncluded()) {
               output.beginRow();
            
               output.beginCell(CssClass.PACKAGE_SUMMARY_LEFT);
               printType(output, classDoc);
               output.endCell();

               output.beginCell(CssClass.PACKAGE_SUMMARY_RIGHT);
               printTags(output, classDoc, classDoc.firstSentenceTags(), true);
               output.endCell();
               output.endRow();
            }
         }
         output.endTable();
         output.endDiv(CssClass.TABLE_CONTAINER);
         output.print("\n");
      }
   }

   private void printPackagesListFile()
      throws IOException
   {
      PrintWriter out
         = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(getTargetDirectory(),
                                                                                "package-list")),
                                                  "UTF-8"));

      PackageDoc[] packages = getRootDoc().specifiedPackages();
      for (int i=0; i<packages.length; ++i) {
         String packageName = packages[i].name();
         if (packageName.length() > 0) {
            out.println(packageName);
         }
      }

      out.close();
   }

   private void printPackagePage(File packageDir, String pathToRoot, PackageDoc packageDoc)
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(packageDir,
                                              "package-summary" + filenameExtension),
                                     pathToRoot,
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle(packageDoc.name()), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_PACKAGE);
      printNavBarTop(output, "package");

      output.beginDiv(CssClass.PACKAGE_TITLE);
      output.print("Package ");
      if (packageDoc.name().length() > 0) {
         output.print(packageDoc.name());
      }
      else {
         output.print("&lt;Unnamed&gt;");
      }
      output.endDiv(CssClass.PACKAGE_TITLE);

      output.beginDiv(CssClass.PACKAGE_DESCRIPTION_TOP);
      printTags(output, packageDoc, packageDoc.firstSentenceTags(), true);
      output.endDiv(CssClass.PACKAGE_DESCRIPTION_TOP);
      
      printPackagePageClasses(output, packageDoc.interfaces(), 
                              "Interface Summary");
      printPackagePageClasses(output, packageDoc.ordinaryClasses(), 
                              "Class Summary");
      printPackagePageClasses(output, packageDoc.exceptions(), 
                              "Exception Summary");
      printPackagePageClasses(output, packageDoc.errors(), 
                              "Error Summary");

      output.anchorName("description");
      output.beginDiv(CssClass.PACKAGE_DESCRIPTION_FULL);
      printTags(output, packageDoc, packageDoc.inlineTags(), false);
      output.endDiv(CssClass.PACKAGE_DESCRIPTION_FULL);

      printNavBarBottom(output, "package");
      output.endBody();
      output.endPage();
      output.close();
   }

   static class TreeNode
      implements Comparable
   {
      ClassDoc classDoc;
      SortedSet children = new TreeSet();

      TreeNode(ClassDoc classDoc) {
         TreeNode.this.classDoc = classDoc;
      }
      
      public boolean equals(Object other)
      {
         return classDoc.equals(((TreeNode)other).classDoc);
      }

      public int compareTo(Object other)
      {
         return classDoc.compareTo(((TreeNode)other).classDoc);
      }

      public int hashCode()
      {
         return classDoc.hashCode();
      }
   }

   private TreeNode addClassTreeNode(Map treeMap, ClassDoc classDoc)
   {
      TreeNode node = (TreeNode)treeMap.get(classDoc.qualifiedName());
      if (null == node) {
         node = new TreeNode(classDoc);
         treeMap.put(classDoc.qualifiedName(), node);

         ClassDoc superClassDoc = (ClassDoc)classDoc.superclass();
         if (null != superClassDoc) {
            TreeNode parentNode = addClassTreeNode(treeMap, superClassDoc);
            parentNode.children.add(node);
         }
      }
      return node;
   }

   private TreeNode addInterfaceTreeNode(Map treeMap, ClassDoc classDoc)
   {
      TreeNode node = (TreeNode)treeMap.get(classDoc.qualifiedName());
      if (null == node) {
         node = new TreeNode(classDoc);
         treeMap.put(classDoc.qualifiedName(), node);

         ClassDoc[] superInterfaces = classDoc.interfaces();
         if (null != superInterfaces && superInterfaces.length > 0) {
            for (int i=0; i<superInterfaces.length; ++i) {
               TreeNode parentNode = addInterfaceTreeNode(treeMap, superInterfaces[i]);
               parentNode.children.add(node);
            }
         }
         else {
            TreeNode rootNode = (TreeNode)treeMap.get("<root>");
            if (null == rootNode) {
               rootNode = new TreeNode(null);
               treeMap.put("<root>", rootNode);
            }
            rootNode.children.add(node);
         }
      }
      return node;
   }

   private void printPackageTreeRec(HtmlPage output, TreeNode node)
   {
      output.beginElement("li");
      if (node.classDoc.isIncluded()) {
         output.print(node.classDoc.containingPackage().name());
         output.print(".");
         output.beginSpan(CssClass.TREE_LINK);
         printType(output, node.classDoc);
         output.endSpan(CssClass.TREE_LINK);
      }
      else {
         output.print(possiblyQualifiedName(node.classDoc));
      }
      ClassDoc[] interfaces = node.classDoc.interfaces();
      if (interfaces.length > 0) {
         output.print(" (implements ");
         for (int i=0; i<interfaces.length; ++i) {
            ClassDoc implemented = interfaces[i];
            if (i > 0) {
               output.print(", ");
            }
            if (implemented.isIncluded()) {
               output.print(implemented.containingPackage().name());
               output.print(".");
               printType(output, implemented);
            }
            else {
               output.print(possiblyQualifiedName(implemented));
            }
         }
         output.print(" )");
      }

      output.endElement("li");
      output.beginElement("ul");
      Iterator it = node.children.iterator();
      while (it.hasNext()) {
         TreeNode child = (TreeNode)it.next();
         printPackageTreeRec(output, child);
      }
      output.endElement("ul");
   }

   private void printClassTree(HtmlPage output, ClassDoc[] classDocs)
   {      
      Map classTreeMap = new HashMap();

      for (int i=0; i<classDocs.length; ++i) {
         ClassDoc classDoc = classDocs[i];
         if (!classDoc.isInterface()) {
            addClassTreeNode(classTreeMap, classDoc);
         }
      }

      TreeNode root = (TreeNode)classTreeMap.get("java.lang.Object");
      if (null != root) {
         output.div(CssClass.PACKAGE_TREE_SECTION_TITLE, "Class Hierarchy");
         output.beginElement("ul");
         printPackageTreeRec(output, root);
         output.endElement("ul");
      }
   }

   private void printInterfaceTree(HtmlPage output, ClassDoc[] classDocs)
   {
      Map interfaceTreeMap = new HashMap();

      for (int i=0; i<classDocs.length; ++i) {
         ClassDoc classDoc = classDocs[i];
         if (classDoc.isInterface()) {
            addInterfaceTreeNode(interfaceTreeMap, classDoc);
         }
      }

      TreeNode interfaceRoot = (TreeNode)interfaceTreeMap.get("<root>");
      if (null != interfaceRoot) {
         Iterator it = interfaceRoot.children.iterator();
         if (it.hasNext()) {
            output.div(CssClass.PACKAGE_TREE_SECTION_TITLE, "Interface Hierarchy");
            output.beginElement("ul");
            while (it.hasNext()) {
               TreeNode node = (TreeNode)it.next();
               printPackageTreeRec(output, node);
            }
            output.endElement("ul");
         }
      }

   }

   private void printPackageTreePage(File packageDir, String pathToRoot, PackageDoc packageDoc)
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(packageDir,
                                              "tree" + filenameExtension),
                                     pathToRoot,
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle(packageDoc.name() + " Hierarchy"), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_PACKAGE_TREE);
      printNavBarTop(output, "package-tree");

      output.div(CssClass.PACKAGE_TREE_TITLE, "Hierarchy for Package " + packageDoc.name());

      ClassDoc[] classDocs = packageDoc.allClasses();
      printClassTree(output, classDocs);
      printInterfaceTree(output, classDocs);

      printNavBarBottom(output, "package-tree");
      output.endBody();
      output.endPage();
      output.close();
   }

   private void printFullTreePage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "tree" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("Hierarchy"), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_FULL_TREE);
      printNavBarTop(output, "full-tree");

      output.div(CssClass.PACKAGE_TREE_TITLE, "Hierarchy for All Packages");

      output.beginDiv(CssClass.FULL_TREE_PACKAGELIST);
      output.div(CssClass.FULL_TREE_PACKAGELIST_HEADER, "Package Hierarchies:");
      output.beginDiv(CssClass.FULL_TREE_PACKAGELIST_ITEM);
      Set allPackages = getAllPackages();
      Iterator it = allPackages.iterator();
      while (it.hasNext()) {
         PackageDoc packageDoc = (PackageDoc)it.next();
         output.beginAnchor(getPackageURL(packageDoc) + "/tree" + filenameExtension);
         output.print(packageDoc.name());
         output.endAnchor();
         if (it.hasNext()) {
            output.print(", ");
         }
      }
      output.endDiv(CssClass.FULL_TREE_PACKAGELIST_ITEM);
      output.endDiv(CssClass.FULL_TREE_PACKAGELIST);

      ClassDoc[] classDocs = getRootDoc().classes();
      printClassTree(output, classDocs);
      printInterfaceTree(output, classDocs);

      printNavBarBottom(output, "full-tree");
      output.endBody();
      output.endPage();
      output.close();
   }

   private void printIndexEntry(HtmlPage output, Doc entry)
   {
      output.beginDiv(CssClass.INDEX_ENTRY);
      String anchor = null;
      String description = null;
      if (entry instanceof PackageDoc) {
         output.beginAnchor(getPackageURL((PackageDoc)entry) + "package-summary" + filenameExtension);
         output.print(entry.name());
         output.endAnchor();
         output.print(" - package");
      }
      else if (entry instanceof ClassDoc) {
         ClassDoc classDoc = (ClassDoc)entry;
         output.beginAnchor(getClassURL(classDoc));
         output.print(entry.name());
         output.endAnchor();
         output.print(" - ");
         if (entry.isInterface()) {
            output.print("interface ");
         }
         else if (entry.isException()) {
            output.print("exception ");
         }
         else if (entry.isError()) {
            output.print("error ");
         }
         else {
            output.print("class ");
         }
         String packageName = classDoc.containingPackage().name();
         if (packageName.length() > 0) {
            output.print(packageName);
            output.print(".");
         }
         printType(output, classDoc);
      }
      else {
         ProgramElementDoc memberDoc = (ProgramElementDoc)entry;
         output.beginAnchor(getMemberDocURL(output, memberDoc));
         output.print(entry.name());
         if (memberDoc instanceof ExecutableMemberDoc) {
            output.print(((ExecutableMemberDoc)memberDoc).signature());
         }
         output.endAnchor();
         output.print(" - ");

         if (memberDoc.isStatic()) {
            output.print("static ");
         }

         if (entry.isConstructor()) {
            output.print("constructor for class ");
         }
         else if (entry.isMethod()) {
            output.print("method in class ");
         }
         else if (entry.isField()) {
            output.print("field in class ");
         }
         ClassDoc containingClass = memberDoc.containingClass();
         String packageName = containingClass.containingPackage().name();
         if (packageName.length() > 0) {
            output.print(packageName);
            output.print(".");
         }
         printType(output, containingClass);
      }
      output.beginDiv(CssClass.INDEX_ENTRY_DESCRIPTION);
      printTags(output, entry, entry.firstSentenceTags(), true);
      output.endDiv(CssClass.INDEX_ENTRY_DESCRIPTION);
      output.endDiv(CssClass.INDEX_ENTRY);
   }

   private void printFrameSetPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "index" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding(),
                                     HtmlPage.DOCTYPE_FRAMESET);
      
      String title = getWindowTitle();
      output.beginPage(title, getOutputCharset());
      output.beginElement("frameset", "cols", "20%,80%");

      String contentURL;
      if (isSinglePackage()) {
         output.atomicElement("frame", 
                              new String[] { "src", "name" }, 
                              new String[] { getPackageURL(getSinglePackage()) + "/classes" + filenameExtension, "classes" });
         contentURL = getPackageURL(getSinglePackage()) + "/package-summary.html";
      }
      else {
         output.beginElement("frameset", "rows", "25%,75%");
         output.atomicElement("frame", 
                              new String[] { "src", "name" }, 
                              new String[] { "all-packages" + filenameExtension, "packages" });
         output.atomicElement("frame", 
                              new String[] { "src", "name" }, 
                              new String[] { "all-classes" + filenameExtension, "classes" });
         output.endElement("frameset");
         contentURL = "index-noframes" + filenameExtension;
      }
      output.atomicElement("frame", 
                           new String[] { "src", "name" }, 
                           new String[] { contentURL, "content" });
      output.endElement("frameset");
      output.endPage();
      output.close();
   }

   private void printPackagesMenuPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "all-packages" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("Package Menu"), getOutputCharset());
      output.beginBody(CssClass.BODY_MENU_PACKAGES, false);

      output.beginSpan(CssClass.PACKAGE_MENU_ENTRY);
      output.beginAnchor("all-classes" + filenameExtension,
                         null,
                         "classes");
      output.print("All Classes");
      output.endAnchor();
      output.endSpan(CssClass.PACKAGE_MENU_ENTRY);

      output.div(CssClass.PACKAGE_MENU_TITLE, "Packages");

      output.beginDiv(CssClass.PACKAGE_MENU_LIST);

      Set packageDocs = getAllPackages();
      Iterator it = packageDocs.iterator();
      while (it.hasNext()) {
         PackageDoc packageDoc = (PackageDoc)it.next();
         output.beginSpan(CssClass.PACKAGE_MENU_ENTRY);
         output.beginAnchor(getPackageURL(packageDoc) + "/classes" + filenameExtension,
                            null,
                            "classes");
         if (packageDoc.name().length() > 0) {
            output.print(packageDoc.name());
         }
         else {
            output.print("&lt;unnamed package&gt;");
         }
         output.endAnchor();
         output.endSpan(CssClass.PACKAGE_MENU_ENTRY);
         output.br();
      }

      output.endDiv(CssClass.PACKAGE_MENU_LIST);
      output.endBody();
      output.endPage();
      output.close();
   }

   private void printClassMenuEntry(HtmlPage output, ClassDoc classDoc)
   {
      CssClass entryClass;
      if (classDoc.isInterface()) {
         entryClass = CssClass.CLASS_MENU_ENTRY_INTERFACE;        
      }
      else {
         entryClass = CssClass.CLASS_MENU_ENTRY_CLASS;
      }
      output.beginSpan(entryClass);
      output.beginAnchor(getClassDocURL(output, classDoc),
                         classDoc.qualifiedTypeName(),
                         "content");
      output.print(classDoc.name());
      output.endAnchor();
      output.endSpan(entryClass);
      output.br();
   }

   private void printClassMenuSection(HtmlPage output, Collection classDocs, String header)
   {
      if (!classDocs.isEmpty()) {
         output.div(CssClass.CLASS_MENU_SUBTITLE, header);
         Iterator it = classDocs.iterator();
         while (it.hasNext()) {
            ClassDoc classDoc = (ClassDoc)it.next();
            printClassMenuEntry(output, classDoc);
         }
      }
   }

   private void printClassMenuList(HtmlPage output, ClassDoc[] classDocs, boolean categorized)
   {
      output.beginDiv(CssClass.CLASS_MENU_LIST);

      if (categorized) {
         Set classes = new TreeSet();
         Set interfaces = new TreeSet();
         Set exceptions = new TreeSet();
         Set errors = new TreeSet();

         for (int i=0; i<classDocs.length; ++i) {
            ClassDoc classDoc = classDocs[i];
            if (classDoc.isInterface()) {
               interfaces.add(classDoc);
            }
            else if (classDoc.isException()) {
               exceptions.add(classDoc);
            }
            else if (classDoc.isError()) {
               errors.add(classDoc);
            }
            else {
               classes.add(classDoc);
            }
         }
         printClassMenuSection(output, interfaces, "Interfaces");
         printClassMenuSection(output, classes, "Classes");
         printClassMenuSection(output, exceptions, "Exceptions");
         printClassMenuSection(output, errors, "Errors");
      }
      else {
         for (int i=0; i<classDocs.length; ++i) {
            ClassDoc classDoc = classDocs[i];
            if (classDoc.isIncluded()) {
               printClassMenuEntry(output, classDoc);
            }
         }
      }

      output.endDiv(CssClass.CLASS_MENU_LIST);
   }

   private void printAllClassesMenuPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "all-classes" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("Class Menu"), getOutputCharset());
      output.beginBody(CssClass.BODY_MENU_CLASSES, false);

      output.div(CssClass.CLASS_MENU_TITLE, "All Classes");

      printClassMenuList(output, getRootDoc().classes(), false);

      output.endBody();
      output.endPage();
      output.close();
   }

   private void printPackageClassesMenuPage(File packageDir, String pathToRoot, PackageDoc packageDoc)
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(packageDir,
                                              "classes" + filenameExtension),
                                     pathToRoot,
                                     getOutputDocEncoding());

      output.beginPage(getPageTitle(packageDoc.name() + " Class Menu"), getOutputCharset());
      output.beginBody(CssClass.BODY_MENU_CLASSES, false);

      output.beginDiv(CssClass.CLASS_MENU_TITLE);
      output.beginAnchor("package-summary" + filenameExtension, "", "content");
      if (packageDoc.name().length() > 0) {
         output.print(packageDoc.name());
      }
      else {
         output.print("&lt;Unnamed&gt;");
      }
      output.endAnchor();
      output.endDiv(CssClass.CLASS_MENU_TITLE);

      printClassMenuList(output, packageDoc.allClasses(), true);

      output.endBody();
      output.endPage();
      output.close();
   }

   private void printSplitIndex()
      throws IOException
   {
      Map categorizedIndex = getCategorizedIndex();
      Iterator it = categorizedIndex.keySet().iterator();
      int n = 1;
      while (it.hasNext()) {
         Character c = (Character)it.next();
         List classList = (List)categorizedIndex.get(c);
         printIndexPage(n++, c, classList);
      }
   }

   private void printIndexPage()
      throws IOException
   {
      printIndexPage(0, null, null);
   }

   private void printIndexPage(int index, Character letter, List classList)
      throws IOException
   {
      String pageName = "alphaindex";
      if (null != letter) {
         pageName += "-" + index;
      }
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              pageName + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("Alphabetical Index"), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_INDEX);
      printNavBarTop(output, "index");

      {
         output.div(CssClass.INDEX_TITLE, "Alphabetical Index");

         output.beginDiv(CssClass.INDEX_LETTERS);

         Iterator it = getCategorizedIndex().keySet().iterator();
         int n = 1;
         while (it.hasNext()) {
            Character c = (Character)it.next();
            output.beginSpan(CssClass.INDEX_LETTER);
            if (letter != null) {
               output.beginAnchor("alphaindex-" + n + filenameExtension);
            }
            else {
               output.beginAnchor("#" + c);
            }
            output.print(c.toString());
            output.endAnchor();
            output.endSpan(CssClass.INDEX_LETTER);     
            output.beginSpan(CssClass.INDEX_LETTER_SPACER);
            output.print(" ");
            output.endSpan(CssClass.INDEX_LETTER_SPACER);
            ++n;
         }
      }

      output.endDiv(CssClass.INDEX_LETTERS);

      if (null != letter) {
         printIndexCategory(output, letter, classList);
      }
      else {
         Map categorizedIndex = getCategorizedIndex();
         Iterator categoryIt = categorizedIndex.keySet().iterator();

         while (categoryIt.hasNext()) {
            letter = (Character)categoryIt.next();
            classList = (List)categorizedIndex.get(letter);
            output.anchorName(letter.toString());
            printIndexCategory(output, letter, classList);
         }
      }

      printNavBarBottom(output, "index");
      output.endBody();
      output.endPage();
      output.close();
   }

   private void printIndexCategory(HtmlPage output, Character letter, List classList)
   {
      Iterator it = classList.iterator();

      output.div(CssClass.INDEX_CATEGORY_HEADER, letter.toString());
      output.beginDiv(CssClass.INDEX_CATEGORY);
      while (it.hasNext()) {
         Doc entry = (Doc)it.next();
         printIndexEntry(output, entry);
      }
      output.endDiv(CssClass.INDEX_CATEGORY);
   }

   private void printDeprecationSummary(HtmlPage output, List docs, String header)
   {
      if (!docs.isEmpty()) {
         output.beginDiv(CssClass.TABLE_CONTAINER);
         output.beginTable(CssClass.DEPRECATION_SUMMARY, new String[] { "border", "width" }, new String[] { "1", "100%" });
         output.rowDiv(CssClass.TABLE_HEADER, header);

         Iterator it = docs.iterator();
         while (it.hasNext()) {
            Doc doc = (Doc)it.next();
            output.beginRow();
            
            output.beginCell(CssClass.DEPRECATION_SUMMARY_LEFT);
            if (doc instanceof Type) {
               printType(output, (Type)doc);
            }
            else {
               ProgramElementDoc memberDoc = (ProgramElementDoc)doc;
               output.beginAnchor(getMemberDocURL(output, memberDoc));
               output.print(memberDoc.containingClass().qualifiedName());
               output.print(".");
               output.print(memberDoc.name());
               if (memberDoc instanceof ExecutableMemberDoc) {
                  output.print(((ExecutableMemberDoc)memberDoc).flatSignature());
               }
               output.endAnchor();
            }
            output.beginDiv(CssClass.DEPRECATION_SUMMARY_DESCRIPTION);
            printTags(output, doc, doc.tags("deprecated")[0].firstSentenceTags(), true);
            output.endDiv(CssClass.DEPRECATION_SUMMARY_DESCRIPTION);

            output.endCell();

            output.endRow();
         }
         output.endTable();
         output.endDiv(CssClass.TABLE_CONTAINER);
         output.print("\n");
      }
   }


   private void printSerializationPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "serialized-form" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("Serialized Form"), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_DEPRECATED);
      printNavBarTop(output, "serialized");

      output.div(CssClass.SERIALIZED_TITLE, "Serialized Form");

      Iterator it = getAllPackages().iterator();

      while (it.hasNext()) {

         PackageDoc packageDoc = (PackageDoc)it.next();

         List serializableClasses = new LinkedList();
         ClassDoc[] classes = packageDoc.allClasses();
         for (int i=0; i<classes.length; ++i) {
            ClassDoc classDoc = classes[i];
            if (classDoc.isSerializable() || classDoc.isExternalizable()) {
               serializableClasses.add(classDoc);
            }
         }

         if (!serializableClasses.isEmpty()) {
            output.div(CssClass.SERIALIZED_PACKAGE_HEADER, "Package " + packageDoc.name());

            Iterator cit = serializableClasses.iterator();
            while (cit.hasNext()) {
               ClassDoc classDoc = (ClassDoc)cit.next();

               output.anchorName(classDoc.qualifiedTypeName());

               output.beginDiv(CssClass.SERIALIZED_CLASS_HEADER);
               output.print("Class ");
               printType(output, classDoc, true);
               output.print(" extends ");
               printType(output, classDoc.superclass());
               output.print(" implements Serializable");
               output.endDiv(CssClass.SERIALIZED_CLASS_HEADER);

               FieldDoc serialVersionUidField = findField(classDoc, "serialVersionUID");
               if (null != serialVersionUidField
                   && serialVersionUidField.isFinal()
                   && serialVersionUidField.isStatic()
                   && serialVersionUidField.type().typeName().equals("long")) {

                  String fieldValue = serialVersionUidField.constantValueExpression();
                  if (null != fieldValue) {
                     output.beginDiv(CssClass.SERIALIZED_SVUID_OUTER);
                     output.span(CssClass.SERIALIZED_SVUID_HEADER, "serialVersionUID: ");
                     output.span(CssClass.SERIALIZED_SVUID_VALUE, fieldValue);
                     output.endDiv(CssClass.SERIALIZED_SVUID_OUTER);
                  }
               }
               printMemberDetails(output, 
                                  classDoc.serializationMethods(), 
                                  "Serialization Methods",
                                  true);
               printMemberDetails(output, 
                                  classDoc.serializableFields(), 
                                  "Serialized Fields",
                                  true);
            }
         }
      }

      printNavBarBottom(output, "serialized");

      output.endBody();
      output.endPage();
      output.close();
   }


   private void printDeprecationPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "deprecated" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("Deprecated API"), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_DEPRECATED);
      printNavBarTop(output, "deprecated");

      output.div(CssClass.DEPRECATION_TITLE, "Deprecated API");

      List deprecatedInterfaces = new LinkedList();
      List deprecatedExceptions = new LinkedList();
      List deprecatedErrors = new LinkedList();
      List deprecatedClasses = new LinkedList();
      List deprecatedFields = new LinkedList();
      List deprecatedMethods = new LinkedList();
      List deprecatedConstructors = new LinkedList();

      ClassDoc[] classDocs = getRootDoc().classes();
      for (int i=0; i<classDocs.length; ++i) {
         ClassDoc classDoc = classDocs[i];
         {
            Tag[] deprecatedTags = classDoc.tags("deprecated");
            if (null != deprecatedTags && deprecatedTags.length > 0) {
               if (classDoc.isInterface()) {
                  deprecatedInterfaces.add(classDoc);
               }
               else if (classDoc.isException()) {
                  deprecatedExceptions.add(classDoc);
               }
               else if (classDoc.isError()) {
                  deprecatedErrors.add(classDoc);
               }
               else {
                  deprecatedClasses.add(classDoc);
               }
            }
         }
         ConstructorDoc[] constructors = classDoc.constructors();
         for (int j=0; j<constructors.length; ++j) {
            Tag[] deprecatedTags = constructors[j].tags("deprecated");
            if (null != deprecatedTags && deprecatedTags.length > 0) {
               deprecatedConstructors.add(constructors[j]);
            }
         }
         MethodDoc[] methods = classDoc.methods();
         for (int j=0; j<methods.length; ++j) {
            Tag[] deprecatedTags = methods[j].tags("deprecated");
            if (null != deprecatedTags && deprecatedTags.length > 0) {
               deprecatedMethods.add(methods[j]);
            }
         }
         FieldDoc[] fields = classDoc.fields();
         for (int j=0; j<fields.length; ++j) {
            Tag[] deprecatedTags = fields[j].tags("deprecated");
            if (null != deprecatedTags && deprecatedTags.length > 0) {
               deprecatedFields.add(fields[j]);
            }
         }
      }

      output.beginDiv(CssClass.DEPRECATION_TOC);
      output.div(CssClass.DEPRECATION_TOC_HEADER, "Contents");
      output.beginDiv(CssClass.DEPRECATION_TOC_LIST);
      if (!deprecatedInterfaces.isEmpty()) {
         output.beginDiv(CssClass.DEPRECATION_TOC_ENTRY);
         output.anchor("#interfaces", "Deprecated Interfaces");
         output.endDiv(CssClass.DEPRECATION_TOC_ENTRY);
      }
      if (!deprecatedClasses.isEmpty()) {
         output.beginDiv(CssClass.DEPRECATION_TOC_ENTRY);
         output.anchor("#classes", "Deprecated Classes");
         output.endDiv(CssClass.DEPRECATION_TOC_ENTRY);
      }
      if (!deprecatedExceptions.isEmpty()) {
         output.beginDiv(CssClass.DEPRECATION_TOC_ENTRY);
         output.anchor("#exceptions", "Deprecated Exceptions");
         output.endDiv(CssClass.DEPRECATION_TOC_ENTRY);
      }
      if (!deprecatedErrors.isEmpty()) {
         output.beginDiv(CssClass.DEPRECATION_TOC_ENTRY);
         output.anchor("#errors", "Deprecated Errors");
         output.endDiv(CssClass.DEPRECATION_TOC_ENTRY);
      }
      if (!deprecatedFields.isEmpty()) {
         output.beginDiv(CssClass.DEPRECATION_TOC_ENTRY);
         output.anchor("#fields", "Deprecated Fields");
         output.endDiv(CssClass.DEPRECATION_TOC_ENTRY);
      }
      if (!deprecatedFields.isEmpty()) {
         output.beginDiv(CssClass.DEPRECATION_TOC_ENTRY);
         output.anchor("#methods", "Deprecated Methods");
         output.endDiv(CssClass.DEPRECATION_TOC_ENTRY);
      }
      if (!deprecatedFields.isEmpty()) {
         output.beginDiv(CssClass.DEPRECATION_TOC_ENTRY);
         output.anchor("#constructors", "Deprecated Constructors");
         output.endDiv(CssClass.DEPRECATION_TOC_ENTRY);
      }
      output.endDiv(CssClass.DEPRECATION_TOC_LIST);
      output.endDiv(CssClass.DEPRECATION_TOC);

      output.anchorName("interfaces");
      printDeprecationSummary(output, deprecatedInterfaces, "Deprecated Interfaces");

      output.anchorName("classes");
      printDeprecationSummary(output, deprecatedClasses, "Deprecated Classes");

      output.anchorName("exceptions");
      printDeprecationSummary(output, deprecatedExceptions, "Deprecated Exceptions");

      output.anchorName("errors");
      printDeprecationSummary(output, deprecatedErrors, "Deprecated Errors");

      output.anchorName("fields");
      printDeprecationSummary(output, deprecatedFields, "Deprecated Fields");

      output.anchorName("methods");
      printDeprecationSummary(output, deprecatedMethods, "Deprecated Methods");

      output.anchorName("constructors");
      printDeprecationSummary(output, deprecatedConstructors, "Deprecated Constructors");

      printNavBarBottom(output, "deprecated");
      output.endBody();
      output.endPage();
      output.close();
   }

   private void printAboutPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "about" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("About"), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_ABOUT);

      printNavBarTop(output, "about");

      output.beginDiv(CssClass.ABOUT_GENERATOR);
      output.print("Generated by ");
      output.print("GNU Gjdoc");
      output.print(" Standard Doclet ");
      output.print(getDocletVersion());
      output.endDiv(CssClass.ABOUT_GENERATOR);

      printNavBarBottom(output, "about");

      output.endBody();
      output.endPage();
      output.close();
   }

   private void printHelpPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "help" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle("Help"), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_HELP);

      printNavBarTop(output, "help");

      InputStream helpIn;
      if (null != optionHelpFile.getValue()){ 
         helpIn = new FileInputStream(optionHelpFile.getValue());
      }
      else {
         helpIn = getClass().getResourceAsStream("/htmldoclet/help.xhtml");
      }
      output.insert(new InputStreamReader(helpIn, "utf-8"));
      helpIn.close();

      printNavBarBottom(output, "help");

      output.endBody();
      output.endPage();
      output.close();
   }

   private void printOverviewPage()
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(getTargetDirectory(),
                                              "index-noframes" + filenameExtension),
                                     ".",
                                     getOutputDocEncoding());
      output.beginPage(getWindowTitle(), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_OVERVIEW);

      printNavBarTop(output, "overview");

      String overviewHeader;
      if (null != optionDocTitle.getValue()) {
         overviewHeader = optionDocTitle.getValue();
      }
      else if (null != optionTitle.getValue()) {
         overviewHeader = optionTitle.getValue();
      }
      else {
         overviewHeader = null;
      }

      if (null != overviewHeader) {
         output.div(CssClass.OVERVIEW_TITLE, overviewHeader);
      }

      /*
      output.beginDiv(CssClass.PACKAGE_DESCRIPTION_TOP);
      printTags(output, packageDoc.firstSentenceTags(), true);
      output.endDiv(CssClass.PACKAGE_DESCRIPTION_TOP);
      */

      List packageGroups = getPackageGroups();

      if (packageGroups.isEmpty()) {
      
         printOverviewPackages(output, getAllPackages(),
                               "All Packages");
      }
      else {
         Set otherPackages = new LinkedHashSet();
         otherPackages.addAll(getAllPackages());

         Iterator it = packageGroups.iterator();
         while (it.hasNext()) {
            PackageGroup packageGroup = (PackageGroup)it.next();
            printOverviewPackages(output, 
                                  packageGroup.getPackages(),
                                  packageGroup.getName());
            otherPackages.removeAll(packageGroup.getPackages());
         }

         if (!otherPackages.isEmpty()) {
            printOverviewPackages(output, 
                                  otherPackages,
                                  "Other Packages");
         }
      }

      /*
      output.anchorName("description");
      output.beginDiv(CssClass.PACKAGE_DESCRIPTION_FULL);
      printTags(output, packageDoc.inlineTags(), false);
      output.endDiv(CssClass.PACKAGE_DESCRIPTION_FULL);
      */

      printNavBarBottom(output, "overview");
      output.endBody();
      output.endPage();
      output.close();
   }

   private void printOverviewPackages(HtmlPage output, Collection packageDocs, String header)
   {
      output.beginDiv(CssClass.TABLE_CONTAINER);
      output.beginTable(CssClass.OVERVIEW_SUMMARY, new String[] { "border", "width" }, new String[] { "1", "100%" });
      output.rowDiv(CssClass.TABLE_HEADER, header);

      Iterator it = packageDocs.iterator();
      while (it.hasNext()) {
         PackageDoc packageDoc = (PackageDoc)it.next();
         output.beginRow();
         
         output.beginCell(CssClass.OVERVIEW_SUMMARY_LEFT);
         output.beginAnchor(getPackageURL(packageDoc) + "/package-summary" + filenameExtension);
         output.print(packageDoc.name());
         output.endAnchor();
         output.endCell();

         output.beginCell(CssClass.OVERVIEW_SUMMARY_RIGHT);
         printTags(output, packageDoc, packageDoc.firstSentenceTags(), true);
         output.endCell();
         output.endRow();
      }
      output.endTable();
      output.endDiv(CssClass.TABLE_CONTAINER);
   }

   private void printClassUsagePage(File packageDir, String pathToRoot, ClassDoc classDoc)
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(packageDir,
                                              classDoc.name() + "-uses" + filenameExtension),
                                     pathToRoot,
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle(classDoc.name()), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_USES);
      printNavBarTop(output, "uses", classDoc);

      output.div(CssClass.USAGE_TITLE, "Uses of class " + classDoc.qualifiedName());

      Map packageToUsageTypeMap = getUsageOfClass(classDoc);
      if (null != packageToUsageTypeMap) {

         Iterator packagesIterator = packageToUsageTypeMap.keySet().iterator();
         while (packagesIterator.hasNext()) {
            PackageDoc packageDoc = (PackageDoc)packagesIterator.next();

            output.div(CssClass.USAGE_PACKAGE_TITLE, "Uses in package " + packageDoc.name());

            Map usageTypeToUsersMap = (Map)packageToUsageTypeMap.get(packageDoc);
            Iterator usageTypeIterator = usageTypeToUsersMap.keySet().iterator();
            while (usageTypeIterator.hasNext()) {
               UsageType usageType = (UsageType)usageTypeIterator.next();
               
               output.endDiv(CssClass.TABLE_CONTAINER);
               output.beginTable(CssClass.USAGE_SUMMARY, new String[] { "border", "width" }, new String[] { "1", "100%" });
               output.rowDiv(CssClass.TABLE_HEADER, format("usagetype." + usageType.getId(), 
                                                           classDoc.qualifiedName()));

               Set users = (Set)usageTypeToUsersMap.get(usageType);
               Iterator userIterator = users.iterator();
               while (userIterator.hasNext()) {
                  Doc user = (Doc)userIterator.next();
                  if (user instanceof ClassDoc) {
                     output.beginCell(CssClass.USAGE_SUMMARY_LEFT);
                     output.print("class");
                     output.endCell();

                     output.beginCell(CssClass.USAGE_SUMMARY_RIGHT);
                     printType(output, ((ClassDoc)user));
                     output.endCell();
                  }
                  else if (user instanceof FieldDoc) {
                     FieldDoc fieldDoc = (FieldDoc)user;

                     output.beginCell(CssClass.USAGE_SUMMARY_LEFT);
                     printType(output, ((FieldDoc)user).type());
                     output.endCell();

                     output.beginCell(CssClass.USAGE_SUMMARY_RIGHT);
                     output.beginAnchor(getMemberDocURL(output, (FieldDoc)user));
                     output.print(((FieldDoc)user).name());
                     output.endAnchor();
                     output.endCell();
                  }
                  else if (user instanceof MethodDoc) {
                     MethodDoc methodDoc = (MethodDoc)user;

                     output.beginCell(CssClass.USAGE_SUMMARY_LEFT);
                     printType(output, ((MethodDoc)user).returnType());
                     output.endCell();

                     output.beginCell(CssClass.USAGE_SUMMARY_RIGHT);
                     output.beginAnchor(getMemberDocURL(output, (MethodDoc)user));
                     output.print(((MethodDoc)user).name());
                     output.endAnchor();
                     printParameters(output, (ExecutableMemberDoc)user);
                     output.endCell();
                  }
                  else if (user instanceof ConstructorDoc) {
                     ConstructorDoc constructorDoc = (ConstructorDoc)user;

                     output.beginCell(CssClass.USAGE_SUMMARY_LEFT);
                     output.endCell();

                     output.beginCell(CssClass.USAGE_SUMMARY_RIGHT);
                     output.beginAnchor(getMemberDocURL(output, (ConstructorDoc)user));
                     output.print(((ConstructorDoc)user).name());
                     output.endAnchor();
                     printParameters(output, (ExecutableMemberDoc)user);
                     output.endCell();
                  }
               }
            }
         }
      }
      printNavBarBottom(output, "uses", classDoc);
      output.endBody();
      output.endPage();
      output.close();
   }

   private void printSuperTreeRec(HtmlPage output, ListIterator it, int level)
   {
      if (it.hasPrevious()) {
         ClassDoc cd = (ClassDoc)it.previous();
         output.beginElement("li", new String[] { "class" }, new String[] { "inheritance " + level });
         output.beginElement("code");
         if (it.hasPrevious()) {
            printType(output, cd, true);
         }
         else {
            output.print(cd.qualifiedName());
         }
         output.endElement("code");
         output.endElement("li");

         if (it.hasPrevious()) {
            output.beginElement("ul", new String[] { "class" }, new String[] { "inheritance " + (level + 1) });
            printSuperTreeRec(output, it, level + 1);
            output.endElement("ul");
         }
      }
   }

   private static boolean isSubInterface(ClassDoc classDoc, ClassDoc otherClassDoc) 
   {
      ClassDoc[] interfaces = otherClassDoc.interfaces();
      for (int i=0; i<interfaces.length; ++i) {
         if (classDoc == interfaces[i]) {
            return true;
         }
         else if (isSubInterface(classDoc, interfaces[i])) {
            return true;
         }
      }
      return false;
   }

   private void printCommaSeparatedTypes(HtmlPage output,
                                         Collection list, 
                                         String header, 
                                         CssClass cssClass)
   {
      if (!list.isEmpty()) {
         output.beginDiv(cssClass);
         output.div(CssClass.CLASS_KNOWNIMPLEMENTING_HEADER, header);
         output.beginDiv(CssClass.CLASS_KNOWNIMPLEMENTING_ITEM);
         Iterator it = list.iterator();
         while (it.hasNext()) {
            Type type = (Type)it.next();
            printType(output, type);
            if (it.hasNext()) {
               output.print(", ");
            }
         }
         output.endDiv(CssClass.CLASS_KNOWNIMPLEMENTING_ITEM);
         output.endDiv(cssClass);
      }
   }

   private void printClassPage(File packageDir, String pathToRoot, ClassDoc classDoc)
      throws IOException
   {
      HtmlPage output = new HtmlPage(new File(packageDir,
                                              classDoc.name() + filenameExtension),
                                     pathToRoot,
                                     getOutputDocEncoding());
      output.beginPage(getPageTitle(classDoc.name()), getOutputCharset());
      output.beginBody(CssClass.BODY_CONTENT_CLASS);
      printNavBarTop(output, "class", classDoc);
      
      output.beginDiv(CssClass.CLASS_TITLE);
      output.div(CssClass.CLASS_TITLE_PACKAGE, 
                 classDoc.containingPackage().name());
      output.div(CssClass.CLASS_TITLE_CLASS, 
                 getClassTypeName(classDoc) 
                 + " " + classDoc.name());
      output.endDiv(CssClass.CLASS_TITLE);

      if (classDoc.isInterface()) {

         InterfaceRelation relation
            = (InterfaceRelation)getInterfaceRelations().get(classDoc);

         printCommaSeparatedTypes(output,
                                  relation.superInterfaces, 
                                  "All Superinterfaces:",
                                  CssClass.CLASS_KNOWNIMPLEMENTING);

         printCommaSeparatedTypes(output,
                                  relation.subInterfaces, 
                                  "Known Subinterfaces:",
                                  CssClass.CLASS_KNOWNIMPLEMENTING);

         printCommaSeparatedTypes(output,
                                  relation.implementingClasses, 
                                  "Known Implementing Classes:",
                                  CssClass.CLASS_KNOWNIMPLEMENTING);
      }
      else {   
         LinkedList superClasses = new LinkedList();
         for (ClassDoc cd = classDoc; cd != null; cd = cd.superclass()) {
            superClasses.add(cd);
         }
         output.beginDiv(CssClass.CLASS_INHERITANCETREE);
         output.beginElement("ul", new String[] { "class" }, new String[] { "inheritance 0" });
         printSuperTreeRec(output, superClasses.listIterator(superClasses.size()), 0);
         output.endElement("ul");
         output.endDiv(CssClass.CLASS_INHERITANCETREE);

         if (null != classDoc.containingClass()) {
            output.beginDiv(CssClass.CLASS_ENCLOSINGCLASS);
            output.div(CssClass.CLASS_ENCLOSINGCLASS_HEADER, "Enclosing Class:");
            output.beginDiv(CssClass.CLASS_ENCLOSINGCLASS_ITEM);
            printType(output, classDoc.containingClass());
            output.endDiv(CssClass.CLASS_ENCLOSINGCLASS_ITEM);
            output.endDiv(CssClass.CLASS_ENCLOSINGCLASS);
         }

         Set implementedInterfaces = getImplementedInterfaces(classDoc);

         printCommaSeparatedTypes(output,
                                  implementedInterfaces, 
                                  "Implemented Interfaces:",
                                  CssClass.CLASS_KNOWNIMPLEMENTING);


         List knownDirectSubclasses = getKnownDirectSubclasses(classDoc);
         if (!knownDirectSubclasses.isEmpty()) {
            output.beginDiv(CssClass.CLASS_SUBCLASSES);
            output.div(CssClass.CLASS_SUBCLASSES_HEADER, "Known Direct Subclasses:");
            output.beginDiv(CssClass.CLASS_SUBCLASSES_ITEM);
            Iterator it = knownDirectSubclasses.iterator();
            while (it.hasNext()) {
               printType(output, (ClassDoc)it.next());
               if (it.hasNext()) {
                  output.print(", ");
               }
            }
            output.endDiv(CssClass.CLASS_SUBCLASSES_ITEM);
            output.endDiv(CssClass.CLASS_SUBCLASSES_HEADER);
            output.endDiv(CssClass.CLASS_SUBCLASSES);
         }
      }

      output.hr();

      output.beginDiv(CssClass.CLASS_SYNOPSIS);
      output.beginDiv(CssClass.CLASS_SYNOPSIS_DECLARATION);
      output.print(getFullModifiers(classDoc) + ' ' + getClassTypeKeyword(classDoc) 
                   + ' ');
      output.beginSpan(CssClass.CLASS_SYNOPSIS_NAME);
      if (optionLinkSource.getValue() && null != classDoc.position()) {
         output.beginAnchor(getOuterClassDoc(classDoc).name() + "-source" + filenameExtension + "#line." + classDoc.position());
         output.print(classDoc.name());
         output.endAnchor();
      }
      else {
         output.print(classDoc.name());
      }
      output.endSpan(CssClass.CLASS_SYNOPSIS_NAME);
      output.endDiv(CssClass.CLASS_SYNOPSIS_DECLARATION);

      if (!classDoc.isInterface()) {
         if (null != classDoc.superclass()) {
            output.beginDiv(CssClass.CLASS_SYNOPSIS_SUPERCLASS);
            output.print("extends ");
            printType(output, classDoc.superclass());
            output.endDiv(CssClass.CLASS_SYNOPSIS_SUPERCLASS);
         }
      }

      ClassDoc[] interfaces = classDoc.interfaces();
      if (interfaces.length > 0) {
         output.beginDiv(CssClass.CLASS_SYNOPSIS_IMPLEMENTS);
         if (!classDoc.isInterface()) {
            output.print("implements ");
         }
         else {
            output.print("extends ");
         }
         for (int i=0; i<interfaces.length; ++i) {
            if (i>0) {
               output.print(", ");
            }
            printType(output, interfaces[i]);
         }
         output.endDiv(CssClass.CLASS_SYNOPSIS_IMPLEMENTS);
      }
      output.endDiv(CssClass.CLASS_SYNOPSIS);

      output.hr();

      if (!optionNoComment.getValue()) {
         output.beginDiv(CssClass.CLASS_DESCRIPTION);
         printTags(output, classDoc, classDoc.inlineTags(), false);
         output.endDiv(CssClass.CLASS_DESCRIPTION);

         printTaglets(output, classDoc.tags(), new HtmlTagletContext(classDoc, output, false));
      }


      Set implementedInterfaces = getImplementedInterfaces(classDoc);

      boolean haveInheritedFields = false;
      boolean haveInheritedMethods = false;
      boolean haveInheritedClasses = false;
      {
         Iterator it = implementedInterfaces.iterator();
         while (it.hasNext() && !haveInheritedClasses) {
            ClassDoc implementedInterface 
               = (ClassDoc)it.next();
            if (!"java.io.Serializable".equals(implementedInterface.qualifiedName())
                && !"java.io.Externalizable".equals(implementedInterface.qualifiedName())) {
               haveInheritedClasses = true;
            }
         }

         ClassDoc superClassDoc = classDoc.superclass();
         while (null != superClassDoc
                && (!haveInheritedFields 
                    || !haveInheritedMethods
                    || !haveInheritedClasses)) {
            if (superClassDoc.fields().length > 0) {
               haveInheritedFields = true;
            }
            if (superClassDoc.methods().length > 0) {
               haveInheritedMethods = true;
            }
            if (superClassDoc.innerClasses().length > 0) {
               haveInheritedClasses = true;
            }
            superClassDoc = superClassDoc.superclass();
         }
      }
      
      printProgramElementDocs(output, getSortedInnerClasses(classDoc), 
                              "Nested Class Summary", haveInheritedClasses);

      {
         ClassDoc superClassDoc = classDoc.superclass();
         while (null != superClassDoc) {
            printInheritedMembers(output, getSortedInnerClasses(superClassDoc),
                                  "Nested classes/interfaces inherited from class {0}",
                                  superClassDoc);
            superClassDoc = superClassDoc.superclass();
         }
      }

      printProgramElementDocs(output, getSortedFields(classDoc), 
                              "Field Summary", haveInheritedFields);

      {
         ClassDoc superClassDoc = classDoc.superclass();
         while (null != superClassDoc) {
            printInheritedMembers(output, getSortedFields(superClassDoc),
                                  "Fields inherited from class {0}",
                                  superClassDoc);
            superClassDoc = superClassDoc.superclass();
         }
      }

      {
         Iterator it = implementedInterfaces.iterator();
         while (it.hasNext()) {
            ClassDoc implementedInterface 
               = (ClassDoc)it.next();
            if (!"java.io.Serializable".equals(implementedInterface.qualifiedName())
                && !"java.io.Externalizable".equals(implementedInterface.qualifiedName())) {
               printInheritedMembers(output, getSortedFields(implementedInterface),
                                     "Fields inherited from interface {0}",
                                     implementedInterface);
            }
         } 
      }

      printProgramElementDocs(output, getSortedConstructors(classDoc), 
                              "Constructor Summary", false);
      printProgramElementDocs(output, getSortedMethods(classDoc),
                              "Method Summary", haveInheritedMethods);

      if (classDoc.isInterface()) {
         InterfaceRelation relation 
            = (InterfaceRelation)getInterfaceRelations().get(classDoc);
         Iterator it = relation.superInterfaces.iterator();
         while (it.hasNext()) {
            ClassDoc superClassDoc = (ClassDoc)it.next();
            printInheritedMembers(output, getSortedMethods(superClassDoc),
                                  "Methods inherited from interface {0}",
                                  superClassDoc);
         }
      }
      else {
         ClassDoc superClassDoc = classDoc.superclass();
         while (null != superClassDoc) {
            printInheritedMembers(output, getSortedMethods(superClassDoc),
                                  "Methods inherited from class {0}",
                                  superClassDoc);
            superClassDoc = superClassDoc.superclass();
         }
      }

      printMemberDetails(output, getSortedFields(classDoc), 
                         "Field Details", false);
      printMemberDetails(output, getSortedConstructors(classDoc), 
                         "Constructor Details", false);
      printMemberDetails(output, getSortedMethods(classDoc),
                         "Method Details", false);

      printNavBarBottom(output, "class", classDoc);

      output.endBody();
      output.endPage();
      output.close();
   }

   private void printInheritedMembers(HtmlPage output,
                                      ProgramElementDoc[] memberDocs, 
                                      String headerFormat,
                                      ClassDoc superclass)
   {
      if (memberDocs.length > 0) {

         output.beginDiv(CssClass.TABLE_CONTAINER);
         output.beginTable(CssClass.CLASS_SUMMARY, new String[] { "border", "width" }, new String[] { "1", "100%" });
         String superclassLink;
         if (superclass.isIncluded()) {
            superclassLink = superclass.containingPackage().name()
               + "." + createTypeHref(output, superclass, false);
         }
         else {
            superclassLink = createTypeHref(output, superclass, true);
         }
         output.rowDiv(CssClass.TABLE_SUB_HEADER, 
                       new MessageFormat(headerFormat).format(new Object[] { 
                          superclassLink 
                       }));

         output.beginRow();
         output.beginCell(CssClass.CLASS_SUMMARY_INHERITED);
         for (int i=0; i<memberDocs.length; ++i) {
            ProgramElementDoc memberDoc = memberDocs[i];
            if (i > 0) {
               output.print(", ");
            }
            String title = null;
            if (memberDoc.isMethod()) {
               title = memberDoc.name() + ((MethodDoc)memberDoc).flatSignature();
            }
            else if (memberDoc.isInterface()) {
               title = "interface " + ((ClassDoc)memberDoc).qualifiedName();
            } 
            else if (memberDoc.isClass()) {
               title = "class " + ((ClassDoc)memberDoc).qualifiedName();
            }
            output.beginAnchor(getMemberDocURL(output, memberDoc), title);
            output.beginSpan(CssClass.CLASS_SUMMARY_INHERITED_MEMBER);
            output.print(memberDoc.name());
            output.endSpan(CssClass.CLASS_SUMMARY_INHERITED_MEMBER);
            output.endAnchor();
         }
         output.endCell();
         output.endRow();
         output.endTable();
         output.endDiv(CssClass.TABLE_CONTAINER);
      }
   }

   private void collectSpecifiedByRecursive(Set specifyingInterfaces, 
                                            ClassDoc classDoc,
                                            MethodDoc methodDoc)
   {
      ClassDoc[] interfaces = classDoc.interfaces();
      for (int i=0; i<interfaces.length; ++i) {
         MethodDoc[] methods = interfaces[i].methods();
         for (int j=0; j<methods.length; ++j) {
            if (methods[j].name().equals(methodDoc.name())
                && methods[j].signature().equals(methodDoc.signature())) {
               specifyingInterfaces.add(methods[j]);
               break;
            }
         }
         collectSpecifiedByRecursive(specifyingInterfaces,
                                     interfaces[i],
                                     methodDoc);
      }
   }

   private void printMemberDetails(HtmlPage output,
                                   ProgramElementDoc[] memberDocs, String header,
                                   boolean isOnSerializedPage)
   {
      if (memberDocs.length > 0) {
         CssClass sectionClass;
         if (isOnSerializedPage) {
            sectionClass = CssClass.SERIALIZED_SECTION_HEADER;
         }
         else {
            sectionClass = CssClass.SECTION_HEADER;
         }
         output.div(sectionClass, header);

         for (int i=0; i<memberDocs.length; ++i) {
            if (i>0) {
               output.hr();
            }

            ProgramElementDoc memberDoc = memberDocs[i];

            output.anchorName(getMemberAnchor(memberDoc));

            output.beginDiv(CssClass.MEMBER_DETAIL);
            output.div(CssClass.MEMBER_DETAIL_NAME, memberDoc.name());

            output.beginDiv(CssClass.MEMBER_DETAIL_SYNOPSIS);
            if (!isOnSerializedPage || !memberDoc.isField()) {
               output.print(getFullModifiers(memberDoc));
            }
            if (memberDoc.isMethod()) {
               output.print(" ");
               printType(output, ((MethodDoc)memberDoc).returnType());
            }
            else if (memberDoc.isField()) {
               output.print(" ");
               printType(output, ((FieldDoc)memberDoc).type());
            }
            output.print(" ");

            if (optionLinkSource.getValue() && null != memberDoc.position()) {
               ClassDoc containingClass = memberDoc.containingClass();
               while (null != containingClass.containingClass()) {
                  containingClass = containingClass.containingClass();
               }
               String href = containingClass.name() + "-source" + filenameExtension + "#line." + memberDoc.position().line();
               output.beginAnchor(href);
               output.print(memberDoc.name());
               output.endAnchor();
            }
            else {
               output.print(memberDoc.name());
            }

            if (memberDoc.isConstructor() || memberDoc.isMethod()) {
               printParameters(output, (ExecutableMemberDoc)memberDoc);
            }
            output.endDiv(CssClass.MEMBER_DETAIL_SYNOPSIS);

            output.beginDiv(CssClass.MEMBER_DETAIL_BODY);

            Tag[] deprecatedTags = memberDoc.tags("deprecated");
            if (deprecatedTags.length > 0) {
               output.beginDiv(CssClass.DEPRECATED_INLINE);
               output.beginSpan(CssClass.DEPRECATED_HEADER);
               output.print("Deprecated. ");
               output.endSpan(CssClass.DEPRECATED_HEADER);
               output.beginSpan(CssClass.DEPRECATED_BODY);
            }
            for (int j=0; j<deprecatedTags.length; ++j) {
               printTags(output, memberDoc, deprecatedTags[j].inlineTags(), true);
            }
            if (deprecatedTags.length > 0) {
               output.endSpan(CssClass.DEPRECATED_BODY);
               output.beginDiv(CssClass.DEPRECATED_INLINE);
            }

            output.beginDiv(CssClass.MEMBER_DETAIL_DESCRIPTION);
            printTags(output, memberDoc, memberDoc.inlineTags(), false);
            output.endDiv(CssClass.MEMBER_DETAIL_DESCRIPTION);

            if (memberDoc.isConstructor() || memberDoc.isMethod()) {

               if (memberDoc.isMethod()) {
                  Set specifyingInterfaces = new LinkedHashSet();
                  if (memberDoc.containingClass().isInterface()) {
                     collectSpecifiedByRecursive(specifyingInterfaces,
                                                 memberDoc.containingClass(), 
                                                 (MethodDoc)memberDoc);
                  }
                  else {
                     for (ClassDoc cd = memberDoc.containingClass();
                          null != cd; cd = cd.superclass()) {
                        collectSpecifiedByRecursive(specifyingInterfaces,
                                                    cd, 
                                                    (MethodDoc)memberDoc);
                     }
                  }

                  if (!specifyingInterfaces.isEmpty()
                      && !isOnSerializedPage) {
                     output.beginDiv(CssClass.MEMBER_DETAIL_SPECIFIED_BY_LIST);
                     output.div(CssClass.MEMBER_DETAIL_SPECIFIED_BY_HEADER, "Specified by:");
                     Iterator it = specifyingInterfaces.iterator();
                     while (it.hasNext()) {
                        MethodDoc specifyingInterfaceMethod = (MethodDoc)it.next();
                        output.beginDiv(CssClass.MEMBER_DETAIL_SPECIFIED_BY_ITEM);
                        output.beginAnchor(getMemberDocURL(output, 
                                                           specifyingInterfaceMethod));
                        output.print(memberDoc.name());
                        output.endAnchor();
                        output.print(" in interface ");
                        printType(output, specifyingInterfaceMethod.containingClass());
                        output.endDiv(CssClass.MEMBER_DETAIL_SPECIFIED_BY_ITEM);
                     }
                     output.endDiv(CssClass.MEMBER_DETAIL_SPECIFIED_BY_LIST);
                  }
                  
                  ClassDoc overriddenClassDoc = null;

                  for (ClassDoc superclassDoc = memberDoc.containingClass().superclass();
                       null != superclassDoc && null == overriddenClassDoc;
                       superclassDoc = superclassDoc.superclass()) {
                     
                     MethodDoc[] methods = superclassDoc.methods();
                     for (int j=0; j<methods.length; ++j) {
                        if (methods[j].name().equals(memberDoc.name())
                            && methods[j].signature().equals(((MethodDoc)memberDoc).signature())) {
                           overriddenClassDoc = superclassDoc;
                           break;
                        }
                     }
                  }

                  if (null != overriddenClassDoc) {
                     output.beginDiv(CssClass.MEMBER_DETAIL_OVERRIDDEN_LIST);
                     output.div(CssClass.MEMBER_DETAIL_OVERRIDDEN_HEADER, "Overrides:");
                     output.beginDiv(CssClass.MEMBER_DETAIL_OVERRIDDEN_ITEM);

                     output.print(memberDoc.name() + " in interface ");
                     printType(output, overriddenClassDoc);

                     output.endDiv(CssClass.MEMBER_DETAIL_OVERRIDDEN_ITEM);
                     output.endDiv(CssClass.MEMBER_DETAIL_OVERRIDDEN_LIST);
                  }
               }

               if (!optionNoComment.getValue()) {

                  ExecutableMemberDoc execMemberDoc
                     = (ExecutableMemberDoc)memberDoc;

                  if (execMemberDoc.paramTags().length > 0) {
                     output.beginDiv(CssClass.MEMBER_DETAIL_PARAMETER_LIST);
                     output.div(CssClass.MEMBER_DETAIL_PARAMETER_HEADER, "Parameters:");
                     Parameter[] parameters = execMemberDoc.parameters();
                     for (int j=0; j<parameters.length; ++j) {
                        Parameter parameter = parameters[j];
                        ParamTag[] paramTags = execMemberDoc.paramTags();
                        ParamTag paramTag = null;
                        for (int k=0; k<paramTags.length; ++k) {
                           if (paramTags[k].parameterName().equals(parameter.name())) {
                              paramTag = paramTags[k];
                              break;
                           }
                        }
                     
                        if (null != paramTag) {
                           output.beginDiv(CssClass.MEMBER_DETAIL_PARAMETER_ITEM);
                           output.beginSpan(CssClass.MEMBER_DETAIL_PARAMETER_ITEM_NAME);
                           output.print(parameter.name());
                           output.endSpan(CssClass.MEMBER_DETAIL_PARAMETER_ITEM_NAME);
                           output.beginSpan(CssClass.MEMBER_DETAIL_PARAMETER_ITEM_SEPARATOR);
                           output.print(" - ");
                           output.endSpan(CssClass.MEMBER_DETAIL_PARAMETER_ITEM_SEPARATOR);
                           output.beginSpan(CssClass.MEMBER_DETAIL_PARAMETER_ITEM_DESCRIPTION);
                           printTags(output, execMemberDoc, paramTag.inlineTags(), false);
                           output.endSpan(CssClass.MEMBER_DETAIL_PARAMETER_ITEM_DESCRIPTION);
                           output.endDiv(CssClass.MEMBER_DETAIL_PARAMETER_ITEM);
                        }
                     }
                     output.endDiv(CssClass.MEMBER_DETAIL_PARAMETER_LIST);
                  }

                  if (execMemberDoc.isMethod() 
                      && !"void".equals(((MethodDoc)execMemberDoc).returnType().typeName())) {

                     Tag[] returnTags = execMemberDoc.tags("return");
                     if (returnTags.length > 0) {
                        Tag returnTag = returnTags[0];

                        output.beginDiv(CssClass.MEMBER_DETAIL_RETURN_LIST);
                        output.div(CssClass.MEMBER_DETAIL_RETURN_HEADER, "Returns:");
                        output.beginDiv(CssClass.MEMBER_DETAIL_RETURN_ITEM);

                        printTags(output, execMemberDoc, returnTag.inlineTags(), false);

                        output.endDiv(CssClass.MEMBER_DETAIL_RETURN_ITEM);
                        output.endDiv(CssClass.MEMBER_DETAIL_RETURN_LIST);
                     }
                  }

                  Set thrownExceptions = getThrownExceptions(execMemberDoc);
                  boolean haveThrowsInfo = false;
                  ThrowsTag[] throwsTags = execMemberDoc.throwsTags();
                  for (int k=0; k<throwsTags.length; ++k) {
                     ThrowsTag throwsTag = throwsTags[k];
                     if (null != throwsTags[k].exception()
                         && (isUncheckedException(throwsTags[k].exception())
                             || thrownExceptions.contains(throwsTag.exception()))) {
                        haveThrowsInfo = true;
                        break;
                     }
                  }

                  if (haveThrowsInfo) {
                     output.beginDiv(CssClass.MEMBER_DETAIL_THROWN_LIST);
                     output.div(CssClass.MEMBER_DETAIL_THROWN_HEADER, "Throws:");

                     for (int k=0; k<throwsTags.length; ++k) {
                        ThrowsTag throwsTag = throwsTags[k];
                        if (null != throwsTag.exception()
                            && (isUncheckedException(throwsTag.exception())
                                || thrownExceptions.contains(throwsTag.exception()))) {
                           output.beginDiv(CssClass.MEMBER_DETAIL_THROWN_ITEM);
                           output.beginSpan(CssClass.MEMBER_DETAIL_THROWN_ITEM_NAME);
                           printType(output, throwsTags[k].exception());
                           output.endSpan(CssClass.MEMBER_DETAIL_THROWN_ITEM_NAME);
                           if (null != throwsTag) {
                              output.beginSpan(CssClass.MEMBER_DETAIL_THROWN_ITEM_SEPARATOR);
                              output.print(" - ");
                              output.endSpan(CssClass.MEMBER_DETAIL_THROWN_ITEM_SEPARATOR);
                              output.beginSpan(CssClass.MEMBER_DETAIL_THROWN_ITEM_DESCRIPTION);
                              printTags(output, execMemberDoc, throwsTag.inlineTags(), false);
                              output.endSpan(CssClass.MEMBER_DETAIL_THROWN_ITEM_DESCRIPTION);
                           }
                           output.endDiv(CssClass.MEMBER_DETAIL_THROWN_ITEM);
                        }
                     }
                     output.endDiv(CssClass.MEMBER_DETAIL_THROWN_LIST);
                  }
               }
            }

            if (!optionNoComment.getValue()) {

               if (memberDoc.isField()) {
                  FieldDoc fieldDoc = ((FieldDoc)memberDoc);
                  if (null != fieldDoc.constantValue()) {
                     output.beginDiv(CssClass.MEMBER_DETAIL_THROWN_LIST);
                     output.div(CssClass.MEMBER_DETAIL_THROWN_HEADER, "Field Value:");
                     output.div(CssClass.MEMBER_DETAIL_THROWN_ITEM, 
                                fieldDoc.constantValueExpression().toString());
                     output.endDiv(CssClass.MEMBER_DETAIL_THROWN_LIST);
                  }
               }

               TagletContext context = new HtmlTagletContext(memberDoc, output, isOnSerializedPage);
               printTaglets(output, memberDoc.tags(), context);
            }

            output.endDiv(CssClass.MEMBER_DETAIL_BODY);
            output.endDiv(CssClass.MEMBER_DETAIL);
         }
      }
   }


   private void printParameters(HtmlPage output, ExecutableMemberDoc memberDoc)
   {
      Parameter[] parameters = memberDoc.parameters();
      output.print("(");
      for (int j=0; j<parameters.length; ++j) {
         if (j > 0) {
            output.print(", ");
         }
         printType(output, parameters[j].type());
         output.print(" ");
         output.print(parameters[j].name());
      }
      output.print(")");
   }

   private void printProgramElementDocs(HtmlPage output,
                                        ProgramElementDoc[] memberDocs, 
                                        String header,
                                        boolean forceOutputHeader)
   {
      if (memberDocs.length > 0 || forceOutputHeader) {
         output.beginDiv(CssClass.TABLE_CONTAINER);
         output.beginTable(CssClass.CLASS_SUMMARY, new String[] { "border", "width" }, new String[] { "1", "100%" });
         output.rowDiv(CssClass.TABLE_HEADER, header);

         for (int i=0; i<memberDocs.length; ++i) {
            ProgramElementDoc memberDoc = memberDocs[i];
            output.beginRow();

            if (!memberDoc.isConstructor()) {
               output.beginCell(CssClass.CLASS_SUMMARY_LEFT);
               output.beginDiv(CssClass.CLASS_SUMMARY_LEFT_SYNOPSIS);
               output.print(getSummaryModifiers(memberDoc) + " ");
               if (memberDoc.isMethod()) {
                  printType(output, ((MethodDoc)memberDoc).returnType());
               }
               else if (memberDoc.isField()) {
                  printType(output, ((FieldDoc)memberDoc).type());
               }
               else if (memberDoc.isInterface()) {
                  output.print(" interface");
               }
               else if (memberDoc.isClass()) {
                  output.print(" class");
               }
               output.endDiv(CssClass.CLASS_SUMMARY_LEFT_SYNOPSIS);
               output.endCell();
            }

            output.beginCell(CssClass.CLASS_SUMMARY_RIGHT);
            output.beginDiv(CssClass.CLASS_SUMMARY_RIGHT_LIST);
            output.beginDiv(CssClass.CLASS_SUMMARY_RIGHT_SYNOPSIS);
            output.beginAnchor("#" + getMemberAnchor(memberDoc));
            output.print(memberDoc.name());
            output.endAnchor();
            if (memberDoc.isConstructor() || memberDoc.isMethod()) {
               printParameters(output, (ExecutableMemberDoc)memberDoc);
            }
            output.endDiv(CssClass.CLASS_SUMMARY_RIGHT_SYNOPSIS);
            Tag[] firstSentenceTags;
            Tag[] deprecatedTags = memberDoc.tags("deprecated");
            if (deprecatedTags.length > 0) {
               firstSentenceTags = deprecatedTags[0].firstSentenceTags();
            }
            else {
               firstSentenceTags = memberDoc.firstSentenceTags();
            }

            if (null != firstSentenceTags && firstSentenceTags.length > 0) {
               output.beginDiv(CssClass.CLASS_SUMMARY_RIGHT_DESCRIPTION);
               if (deprecatedTags.length > 0) {
                  output.beginDiv(CssClass.DEPRECATED);
                  output.beginSpan(CssClass.DEPRECATED_HEADER);
                  output.print("Deprecated. ");
                  output.endSpan(CssClass.DEPRECATED_HEADER);
                  output.beginSpan(CssClass.DEPRECATED_BODY);
               }
               printTags(output, memberDoc, firstSentenceTags, true);
               if (deprecatedTags.length > 0) {
                  output.endSpan(CssClass.DEPRECATED_BODY);
                  output.beginDiv(CssClass.DEPRECATED);
               }
               output.endDiv(CssClass.CLASS_SUMMARY_RIGHT_DESCRIPTION);
            }
            output.endDiv(CssClass.CLASS_SUMMARY_RIGHT_LIST);
            output.endCell();
            output.endRow();
         }
         output.endTable();
         output.endDiv(CssClass.TABLE_CONTAINER);
      }
   }

   private void printTag(final HtmlPage output, 
                         HtmlRepairer repairer,
                         Tag tag, boolean firstSentence,
                         boolean inline,
                         Doc contextDoc)
   {
      TagletContext context = new HtmlTagletContext(contextDoc, output, false);
      if (firstSentence) {
         output.print(renderInlineTags(tag.firstSentenceTags(), context));
      }
      else {
         output.print(renderInlineTags(tag.inlineTags(), context));
      }
   }

   private void printTags(HtmlPage output, Doc contextDoc, Tag[] tags, boolean firstSentence)
   {
      printTags(output, contextDoc, tags, firstSentence, false);
   }

   private void printTags(HtmlPage output, Doc contextDoc, Tag[] tags, boolean firstSentence, boolean inline)
   {
      output.print(renderInlineTags(tags, new HtmlTagletContext(contextDoc, output, false)));
      /*
      if (!optionNoComment.getValue()) {
         output.print(renderInlineTags(tag.firstSentenceTags(), output));
         HtmlRepairer repairer = new HtmlRepairer(getRootDoc(), 
                                                  true, false,
                                                  null, null,
                                                  true);
         for (int i=0; i<tags.length; ++i) {
            printTag(output, repairer, tags[i], firstSentence, inline);
         }
         output.print(repairer.terminateText());
      }
      */
   }

   private String getClassDocURL(HtmlPage output, ClassDoc classDoc)
   {
      return output.getPathToRoot() 
         + "/"
         + getPackageURL(classDoc.containingPackage()) 
         + "/"
         + classDoc.name() + filenameExtension;
   }

   private String getMemberDocURL(HtmlPage output, ProgramElementDoc memberDoc)
   {
      ClassDoc classDoc = memberDoc.containingClass();
      StringBuffer result = new StringBuffer(output.getPathToRoot());
      result.append('/');
      result.append(getPackageURL(classDoc.containingPackage()));
      result.append('/');
      result.append(classDoc.name());
      result.append(filenameExtension);
      result.append('#');
      result.append(memberDoc.name());
      if (memberDoc instanceof ExecutableMemberDoc) {
         result.append(((ExecutableMemberDoc)memberDoc).signature());
      }
      return result.toString();
   }

   private void printType(HtmlPage output, Type type)
   {
      printType(output, type, false);
   }

   private void printType(HtmlPage output, Type type, boolean fullyQualified)
   {
      output.print(createTypeHref(output, type, fullyQualified));
   }

   private String createTypeHref(HtmlPage output, Type type, boolean fullyQualified)
   {
      ClassDoc asClassDoc = type.asClassDoc();
      String url = null;
      if (null != asClassDoc && asClassDoc.isIncluded()) {
         url = getClassDocURL(output, asClassDoc);
      }
      else if (!type.isPrimitive()) {
         if (type.qualifiedTypeName().length() > type.typeName().length()) {
            String packageName = type.qualifiedTypeName();
            packageName = packageName.substring(0, packageName.length() - type.typeName().length() - 1);
            
            ExternalDocSet externalDocSet
               = (ExternalDocSet)packageNameToDocSet.get(packageName);
            if (null != externalDocSet) {
               try {
                  url = externalDocSet.getClassDocURL(packageName, type.typeName());
               }
               catch (MalformedURLException ignore) {
               }
            }
         }
      }

      StringBuffer result = new StringBuffer();
      
      if (null != url && null != asClassDoc) {
         if (fullyQualified) {
            result.append(output.createHrefString(url, possiblyQualifiedName(asClassDoc)));
         }
         else {
            StringBuffer title = new StringBuffer();
            title.append(getClassTypeName(asClassDoc));
            title.append(" in ");
            title.append(asClassDoc.containingPackage().name());
            result.append(output.createHrefString(url, asClassDoc.name(), title.toString()));
         }
      }
      else {
         result.append(possiblyQualifiedName(type));
      }
      result.append(type.dimension());
      return result.toString();
   }

   private void printTaglets(final HtmlPage output, Tag[] tags, TagletContext context) 
   {
      super.printMainTaglets(tags, context, new TagletPrinter() {
            public void printTagletString(String tagletString) {
               output.beginDiv(CssClass.TAGLET);
               output.print(tagletString);
               output.endDiv(CssClass.TAGLET);
            }
         });
   }

   private String getPackageURL(PackageDoc packageDoc)
   {
      if (packageDoc.name().length() > 0) {
         return packageDoc.name().replace('.', '/');
      }
      else {
         return "";
      }
   }

   private String getClassURL(ClassDoc classDoc)
   {
      return getPackageURL(classDoc.containingPackage()) + classDoc.name() + filenameExtension;
   }

   protected void run()
      throws DocletConfigurationException, IOException
   {
      if (optionNoSerialWarn.getValue()) {
         printWarning("option -noserialwarn is currently ignored.");
      }

      if (null != optionTitle.getValue()) {
         printWarning("option -title is deprecated.");
      }

      {
         Iterator it = externalDocSets.iterator();
         while (it.hasNext()) {
            ExternalDocSet externalDocSet = (ExternalDocSet)it.next();
            printNotice("Fetching package list for external documentation set.");     
            try {
               externalDocSet.load(getTargetDirectory());
            }
            catch (FileNotFoundException e) {
               throw new DocletConfigurationException("Cannot fetch package list from " + externalDocSet.getPackageListDir());
            }
            Iterator pit = externalDocSet.getPackageNames().iterator();
            while (pit.hasNext()) {
               String packageName = (String)pit.next();
               packageNameToDocSet.put(packageName, externalDocSet);
            }
         }
      }
      printNotice("Building cross-reference information...");
      getInterfaceRelations();
      getAllSubClasses();

      printNotice("Writing overview files...");
      printFrameSetPage();
      if (!isSinglePackage()) {
         printPackagesMenuPage();
         printAllClassesMenuPage();
         printOverviewPage();
         if (!optionNoTree.getValue()) {
            printNotice("Writing full tree...");
            printFullTreePage();
         }
      }
      printPackagesListFile();
      printAboutPage();
      if (!optionNoIndex.getValue()) {
         printNotice("Writing index...");
         if (!optionSplitIndex.getValue()) {
            printIndexPage();
         }
         else {
            printSplitIndex();
         }
      }
      if (!optionNoHelp.getValue()) {
         printHelpPage();
      }

      // Copy resources

      File resourcesDir = new File(getTargetDirectory(),
                                   "resources");

      if ((resourcesDir.exists() && !resourcesDir.isDirectory())
          || (!resourcesDir.exists() && !resourcesDir.mkdirs())) {
         throw new IOException("Cannot create directory " + resourcesDir);
      }

      // Copy resources

      String[] resourceNames = {
         "gjdoc.js",
         "gjdochtml-clean-layout.css",
         "gjdochtml-clean-color1.css",
         "inherit.png",
      };

      for (int i=0; i<resourceNames.length; ++i) {
         String resourceName = resourceNames[i];
         File targetFile = new File(resourcesDir,
                                    resourceName);
         InputStream in = getClass().getResourceAsStream("/htmldoclet/" + resourceName);
         FileOutputStream out = new FileOutputStream(targetFile);
         IOToolkit.copyStream(in, out);
         in.close();
         out.close();
      }

      // Copy stylesheets

      File stylesheetTargetFile = new File(resourcesDir,
                                           "gjdochtml.css");

      if (null != optionStylesheetFile.getValue()) { 
         IOToolkit.copyFile(optionStylesheetFile.getValue(),
                            stylesheetTargetFile);
      }
      else {
         InputStream cssIn = getClass().getResourceAsStream("/htmldoclet/gjdochtml-vanilla.css");
         FileOutputStream cssOut = new FileOutputStream(stylesheetTargetFile);
         IOToolkit.copyStream(cssIn, cssOut);
         cssIn.close();
         cssOut.close();
      }

      if (!optionNoDeprecatedList.getValue()) {
         printDeprecationPage();
      }

      printSerializationPage();

      Collection packageDocs = getAllPackages();
      Iterator it = packageDocs.iterator();
      while (it.hasNext()) {
         PackageDoc packageDoc = (PackageDoc)it.next();
         File packageDir = new File(getTargetDirectory(),
                                    packageDoc.name().replace('.', File.separatorChar));
         if (!packageDir.exists() && !packageDir.mkdirs()) {
            throw new IOException("Couldn't create directory " + packageDir);
         }
         try {
            List packageSourceDirs = getPackageSourceDirs(packageDoc);
            Iterator pdIt = packageSourceDirs.iterator();
            while (pdIt.hasNext()) {
               File sourcePackageDir = (File)pdIt.next();
               copyDocFiles(sourcePackageDir, packageDir);
            }
         }
         catch (IOException ignore) {
         }
         String pathToRoot = getPathToRoot(packageDir, getTargetDirectory());
         String packageName = packageDoc.name();
         if (0 == packageName.length()) {
            packageName = "<unnamed>";
         }
         printNotice("Writing HTML files for package " + packageName);
         printPackagePage(packageDir, pathToRoot, packageDoc);
         if (!optionNoTree.getValue()) {
            printPackageTreePage(packageDir, pathToRoot, packageDoc);
         }
         printPackageClassesMenuPage(packageDir, pathToRoot, packageDoc);
         ClassDoc[] classDocs = packageDoc.allClasses();
         for (int j=0; j<classDocs.length; ++j) {
            ClassDoc classDoc = classDocs[j];
            if (classDoc.isIncluded()) {
               printClassPage(packageDir, pathToRoot, classDocs[j]);
               if (optionUse.getValue()) {
                  printClassUsagePage(packageDir, pathToRoot, classDocs[j]);
               }
               if (optionLinkSource.getValue() && null == classDoc.containingClass()) {
                  try {
                     File sourceFile = getSourceFile(classDoc);

                     Java2xhtml java2xhtml = new Java2xhtml();
                     Properties properties = new Properties();
                     properties.setProperty("isCodeSnippet", "true");
                     properties.setProperty("hasLineNumbers", "true");
                     java2xhtml.setProperties(properties);
                     
                     StringWriter sourceBuffer = new StringWriter();
                     FileReader sourceReader = new FileReader(sourceFile);
                     IOToolkit.copyStream(sourceReader, sourceBuffer);
                     sourceReader.close();
                     String result = java2xhtml.makeHTML(sourceBuffer.getBuffer(), sourceFile.getName());
                     
                     File targetFile = new File(packageDir,
                                                classDoc.name() + "-source" + filenameExtension);
                     FileWriter targetWriter = new FileWriter(targetFile);
                     targetWriter.write(result);
                     targetWriter.close();
                  }
                  catch (IOException e) {
                     printWarning("Cannot locate source file for class " + classDoc.qualifiedTypeName());
                  }
               }
            }
         }
      }
   }

   private String getPathToRoot(File subDir, File rootDir)
   {
      StringBuffer result = new StringBuffer();
      while (!subDir.equals(rootDir)) {
         if (result.length() > 0) {
            result.append("/");
         }
         subDir = subDir.getParentFile();
         result.append("..");
      }
      if (0 == result.length()) {
         result.append(".");
      }
      return result.toString();
   }

   private String getClassTypeName(ClassDoc classDoc) 
   {
      if (classDoc.isInterface()) {
         return "Interface";
      }
      else {
         return "Class";
      }
   }

   private String getClassTypeKeyword(ClassDoc classDoc) 
   {
      if (classDoc.isInterface()) {
         return "interface";
      }
      else {
         return "class";
      }
   }

   private String getMemberAnchor(ProgramElementDoc memberDoc)
   {
      String anchor = memberDoc.name();
      if (memberDoc.isConstructor() || memberDoc.isMethod()) {
         anchor += ((ExecutableMemberDoc)memberDoc).signature();
      }
      return anchor;
   }

   private String getFullModifiers(ProgramElementDoc memberDoc)
   {
      StringBuffer result = new StringBuffer();
      if (memberDoc.isPackagePrivate()) {
         result.append("(package private) ");
      }
      result.append(memberDoc.modifiers());
      if (memberDoc.isClass() && ((ClassDoc)memberDoc).isAbstract()) {
         result.append(" abstract");
      }
      return result.toString();
   }

   private String getSummaryModifiers(ProgramElementDoc memberDoc)
   {
      StringBuffer result = new StringBuffer();
      if (memberDoc.isPackagePrivate()) {
         result.append("(package private) ");
      }
      else if (memberDoc.isPrivate()) {
         result.append("private ");
      }
      else if (memberDoc.isProtected()) {
         result.append("protected ");
      }
      if (memberDoc.isStatic()) {
         result.append("static");
      }
      else if (memberDoc.isClass() && ((ClassDoc)memberDoc).isAbstract()) {
         result.append("abstract");
      }
      return result.toString();
   }

   protected DocletOption[] getOptions()
   {
      return options;
   }

   private DocletOptionFlag optionNoNavBar = 
     new DocletOptionFlag("-nonavbar");

   private DocletOptionFlag optionNoTree = 
     new DocletOptionFlag("-notree");

   private DocletOptionFlag optionNoDeprecatedList = 
     new DocletOptionFlag("-nodeprecatedlist");

   private DocletOptionFlag optionNoIndex = 
     new DocletOptionFlag("-noindex");

   private DocletOptionFlag optionUse = 
     new DocletOptionFlag("-use");

   private DocletOptionFlag optionNoHelp = 
     new DocletOptionFlag("-nohelp");

   private DocletOptionFlag optionNoComment = 
     new DocletOptionFlag("-nocomment");

   private DocletOptionFlag optionNoSerialWarn = 
     new DocletOptionFlag("-noserialwarn");

   private DocletOptionFlag optionSplitIndex = 
     new DocletOptionFlag("-splitindex");

   private DocletOptionString optionHeader = 
     new DocletOptionString("-header");

   private DocletOptionString optionFooter = 
     new DocletOptionString("-footer");

   private DocletOptionString optionBottom = 
     new DocletOptionString("-bottom");

   private DocletOptionString optionWindowTitle = 
     new DocletOptionString("-windowtitle");

   private DocletOptionString optionDocTitle = 
     new DocletOptionString("-doctitle");

   private DocletOptionString optionTitle = 
     new DocletOptionString("-title");

   private DocletOptionFile optionHelpFile = 
     new DocletOptionFile("-helpfile");

   private DocletOptionFile optionStylesheetFile = 
     new DocletOptionFile("-stylesheetfile");

   private DocletOptionFlag optionLinkSource = 
     new DocletOptionFlag("-linksource");

   private DocletOption optionLink = 
     new DocletOption("-link") {
        
        public int getLength()
        {
           return 2;
        }

        public boolean set(String[] optionArr)
        {
           externalDocSets.add(new ExternalDocSet(optionArr[1], null));
           return true;
        }
     };

   private DocletOption optionLinkOffline = 
     new DocletOption("-linkoffline") {
        
        public int getLength()
        {
           return 3;
        }

        public boolean set(String[] optionArr)
        {
           externalDocSets.add(new ExternalDocSet(optionArr[1], optionArr[2]));
           return true;
        }
     };

   private DocletOptionString optionDocEncoding =
     new DocletOptionString("-docencoding");

   private DocletOptionString optionEncoding =
     new DocletOptionString("-encoding");

   private DocletOptionString optionCharset =
     new DocletOptionString("-charset");

   private DocletOption[] options = 
      {
         optionNoNavBar,
         optionNoTree,
         optionNoDeprecatedList,
         optionNoIndex,
         optionNoHelp,
         optionNoComment,
         optionUse,
         optionSplitIndex,
         optionHeader,
         optionFooter,
         optionBottom,
         optionHelpFile,
         optionStylesheetFile,
         optionWindowTitle,
         optionDocTitle,
         optionLinkSource,
         optionLink,
         optionLinkOffline,
         optionDocEncoding,
         optionEncoding,
         optionCharset,
      };

   static {
      setInstance(new HtmlDoclet());
   }

   private static String replaceDocRoot(HtmlPage output, String str)
   {
      return StringToolkit.replace(str, "{@docRoot}", output.getPathToRoot());
   }

   private String getOutputDocEncoding()
   {
      String encoding = optionDocEncoding.getValue();

      if (null == encoding) {
         encoding = optionEncoding.getValue();
      }

      return encoding;
   }

   private String getOutputCharset()
   {
      return optionCharset.getValue();
   }

   public InlineTagRenderer getInlineTagRenderer()
   {
      return this;
   }

   public String renderInlineTags(Tag[] tags, TagletContext context)
   {
      StringBuffer result = new StringBuffer();

      HtmlRepairer repairer = new HtmlRepairer(getRootDoc(), 
                                               true, false,
                                               null, null,
                                               true);

      for (int i=0; i<tags.length; ++i) {

         Tag tag = tags[i];

         if ("Text".equals(tag.name())) {
            result.append(repairer.getWellformedHTML(tag.text()));
         }
         else if ("@link".equals(tag.name())) {
            result.append(renderSeeTag((SeeTag)tag, context, false));
         }
         else if ("@linkplain".equals(tag.name())) {
            result.append(renderSeeTag((SeeTag)tag, context, true));
         }
         else if ("@docRoot".equals(tag.name())) {         
            result.append(((HtmlTagletContext)context).getOutput().getPathToRoot());
         }
         else {
            //TagletContext context = TagletContext.OVERVIEW; // FIXME
            Taglet taglet = (Taglet)tagletMap.get(tag.name().substring(1));
            if (null != taglet) {
               if (taglet instanceof GnuExtendedTaglet) {
                  result.append(((GnuExtendedTaglet)taglet).toString(tag, context));
               }
               else {
                  result.append(taglet.toString(tag));
               }
            }
         }
      }
      result.append(repairer.terminateText());
      return result.toString();
   }

   public String renderSeeTag(SeeTag seeTag, TagletContext context, boolean plainFont)
   {
      StringBuffer result = new StringBuffer();

      String href = null;
      String label = null;
      MemberDoc referencedMember = seeTag.referencedMember();
      if (null != seeTag.referencedClass()) {
         href = getClassDocURL(((HtmlTagletContext)context).getOutput(), seeTag.referencedClass());

         Doc doc = context.getDoc();
         ClassDoc classDoc = null;
         if (doc.isClass() || doc.isInterface()) {
            classDoc = (ClassDoc)doc;
         }
         else if (doc.isField() || doc.isMethod() || doc.isConstructor()) {
            classDoc = ((MemberDoc)doc).containingClass();
         }

         if (null == referencedMember
             || seeTag.referencedClass() != classDoc
             || ((HtmlTagletContext)context).isOnSerializedPage()) {

            if (!seeTag.referencedClass().isIncluded()) {
               label = possiblyQualifiedName(seeTag.referencedClass());
            }
            else {
               label = seeTag.referencedClass().typeName();
            }
            if (null != referencedMember) {
               label += '.';
            }
         }
         else {
            label = "";
         }

         if (null != referencedMember) {
            href  += '#' + referencedMember.name();
            label += referencedMember.name();
            if (referencedMember.isMethod() || referencedMember.isConstructor()) {
               href += ((ExecutableMemberDoc)referencedMember).signature();
               label += ((ExecutableMemberDoc)referencedMember).flatSignature();
            }
         }
         else if (null != seeTag.referencedMemberName()) {
            href = null;
         }
      }
      
      if (null != seeTag.label()
          && seeTag.label().length() > 0) {
         label = seeTag.label();
      }

      if (null == label) {
         label = seeTag.text();
         if (label.startsWith("#")) {
            label = label.substring(1);
         }
         else {
            label = label.replace('#', '.');
         }
         label.trim();
      }
         
      if (null != href) {
         result.append("<a href=\"");
         result.append(href);
         result.append("\">");
         if (!plainFont) {
            result.append("<code>");
         }
         result.append(label);
         if (!plainFont) {
            result.append("</code>");
         }
         result.append("</a>");
      }
      else {
         if (!plainFont) {
            result.append("<code>");
         }
         result.append(label);
         if (!plainFont) {
            result.append("</code>");
         }
      }

      return result.toString();
   }

   protected String renderTag(String tagName, Tag[] tags, TagletContext context)
   {
      Doc doc = context.getDoc();

      if ("see".equals(tagName)
          && ((tags.length > 0)
              || (doc.isClass()
                  && (((ClassDoc)doc).isSerializable()
                      || ((ClassDoc)doc).isExternalizable())))) {
         
         StringBuffer result = new StringBuffer();
         result.append("<dl class=\"tag list\">");
         result.append("<dt class=\"tag section header\"><b>");
         result.append("See Also:");
         result.append("</b></dt>");

         boolean oneLine = true;

         if (oneLine) {
            result.append("<dd>");
         }

         for (int i = 0; i < tags.length; ++i) {
            if (oneLine) {
               if (i > 0) {
                  result.append(", ");
               }
            }
            else {
               result.append("<dd>");
            }
            result.append(renderSeeTag((SeeTag)tags[i], context, false));
            if (!oneLine) {
               result.append("</dd>");
            }
         }

         if ((doc instanceof ClassDoc)
             && (((ClassDoc)doc).isSerializable() || ((ClassDoc)doc).isExternalizable())) {
            if (tags.length > 0) {
               result.append(", ");
            }
            HtmlPage output = ((HtmlTagletContext)context).getOutput();
            result.append("<a href=\"" + output.getPathToRoot() + "/serialized-form" + filenameExtension + "#" + ((ClassDoc)doc).qualifiedName() + "\">Serialized Form</a>");
         }

         if (oneLine) {
            result.append("<dd>");
         }
         result.append("</dl>");
         return result.toString();
      }
      else if (tags.length > 0 
               && "serial".equals(tagName)
               && ((HtmlTagletContext)context).isOnSerializedPage()) {

         return renderInlineTags(tags[0].inlineTags(), context);
      }
      else {
         return "";
      }
   }

   private String getWindowTitle()
   {
      if (null == optionWindowTitle.getValue()) {
         return "Generated API Documentation";
      }
      else {
         return optionWindowTitle.getValue();
      }
   }

   private String getPageTitle(String title)
   {
      if (null == optionWindowTitle.getValue()) {
         return title;
      }
      else {
         return title + " (" + optionWindowTitle.getValue() + ")";
      }
   }
}