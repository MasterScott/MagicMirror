/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mirror.mirror;

import java.util.logging.Level;
import mirror.mirror.JarHandler;
import java.util.logging.Logger;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javassist.ClassPath;
import javassist.URLClassPath;
import magicmirror.ui.mainUI;
/**
 *
 * @author Machiavelli
 */
    public class functions 
    {

        private static Class mainClazz = null;
        private static Method[] methods = new Method[3000];
        private static Field[] fields = new Field[3000];
        private static List<Class> classes = new ArrayList<>();

        public static Method[][] allMethods = new Method[1500][3000];
        public static Field[][] allFields = new Field[1500][3000];

        public static Method getMethod(String s) {
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(s)) 
                {
                    return methods[i];
                }
            }
            return null;
       }

        public static Class getClass(String s) {
            for (int i = 0; i < classes.size(); i++) {
                if (classes.get(i).getName().equals(s)) 
                {
                    return classes.get(i);
                }
            }
            return null;
       }

        public static int getClassId(String s) {
            for (int i = 0; i < classes.size(); i++) {
                if (classes.get(i).getName().equals(s)) 
                {
                    return i;
                }
            }
            return -1;
       }
        
        public static List<Field> scanInt(int scanVal) {
            List<Field> returnVal = new ArrayList<>();
            int index = 0;
            for (Field[] allField : allFields) {
                for (Field allField1 : allField) {
                    if(allField1 == null)
                       break;
                    
                    if(allField1.getType() == int.class) {
                        allField1.setAccessible(true);
                        try {
                        if(allField1.getInt(classes.get(index)) == scanVal) { //i get better results using newinstance, but it fucks the everything
                            returnVal.add(allField1);
                        } 
                        }catch (Exception ex) {
                            try {
                                if(allField1.getInt(mainClazz) == scanVal) { //i get better results using newinstance, but it fucks the everything
                                    returnVal.add(allField1);
                                } 
                            } catch (Exception ex2) {
                                System.out.println("Something REALLY fucked up");
                            }
                        }
                    }
                }
                index++;
            }
            return returnVal;
        }
        
        public static List<String> getClassNames(String pathToJar) throws FileNotFoundException, IOException {
            List<String> classNames = new ArrayList<String>();
            ZipInputStream zip = new ZipInputStream(new FileInputStream(pathToJar));
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    // This ZipEntry represents a class. Now, what class does it represent?
                    String className = entry.getName().replace('/', '.'); // including ".class"
                    classNames.add(className.substring(0, className.length() - ".class".length()));
                }
            }
            return classNames;
        }

        public void startClient(String cPath, String mC) throws MalformedURLException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
                try {
                    List<String> classNames = getClassNames(cPath);
                    System.out.println(classNames.toString());
                    File cFile = new File(cPath);

                    //add out classpath, and save it so we can reference it later
                    ClassPath cp = mirror.MagicMirror.reflectingPool.appendClassPath(cPath); 
                    CtClass mainClass = mirror.MagicMirror.reflectingPool.get(mC);
                    CtMethod main = mainClass.getDeclaredMethod("main");

                    //code injection is literally this easy
                    //main.insertBefore("{ System.out.println(\"Code injection live!\"); }");

                    //get the modified class bytecode
                    byte[] b = mainClass.toBytecode();
                    //remove the jar from our classpath so we can modify it
                    mirror.MagicMirror.reflectingPool.removeClassPath(cp);

                    //Swap the class in the jar.
                    JarHandler jarHandler = new JarHandler();
                    jarHandler.replaceJarFile(cPath, b, mC.replace(".", "/") + ".class");

                    //Now, we want to start the client. Since we're injecting reflection we could simply start it,
                    //However, we'll reflect it here as well, since it's a faster way to approach scan-by-val.
                    URL[] urls = new URL[]{cFile.toURI().toURL()};
                    ClassLoader cl = new URLClassLoader(urls);
                    //now we want to load the classes from our name array into class objects to reflect, we don't want to just scan the main class!
                    for(int i=0; i<classNames.size(); i++) {
                        System.out.println(classNames.get(i));
                        Class tempClass = cl.loadClass(classNames.get(i));
                        classes.add(tempClass);
                        Method[] iMethods = tempClass.getDeclaredMethods();
                        for(int j=0; j<iMethods.length; j++) {
                            allMethods[i][j] = iMethods[j];
                            allMethods[i][j].setAccessible(true); //duh
                        }
                        
                        Field[] iFields = tempClass.getDeclaredFields();
                        for(int j=0; j<iFields.length; j++) {
                            System.out.println(iFields[j].getName());
                            allFields[i][j] = iFields[j];
                            allFields[i][j].setAccessible(true); //duh
                        }
                        iFields = null;
                        iMethods = null;
                    }
                    //once we have all our classes loaded, we want to grab all the fields and methods to reference later.

                    /*for(int i=0; i<classes.size();i++) {
                        Method[] iMethods = classes.get(i).getDeclaredMethods();
                        for(int j=0; j<iMethods.length; i++) {
                            allMethods[i][j] = iMethods[j];
                            allMethods[i][j].setAccessible(true); //duh
                        }
                        iMethods = null;
                    }
                    for(int i=0; i<classes.size();i++) {
                        Field[] iFields = classes.get(i).getDeclaredFields();
                        for(int j=0; j<iFields.length; i++) {
                            allFields[i][j] = iFields[j];
                            allFields[i][j].setAccessible(true); //duh
                        }
                        iFields = null;
                    }*/

                    //Let's finally get our main method ready to go. We've already called setAccessible, so let's just do it!
                    mainClazz = cl.loadClass(mC);
                    methods = mainClazz.getMethods();
                    fields = mainClazz.getFields();
                    Method invokeMe = getMethod("main");
                    invokeMe.invoke(mainClazz, new Object[] { new String[] { "a", "a" } });


                } catch (NotFoundException ex) {
                    Logger.getLogger(mainUI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(mainUI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(mainUI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (CannotCompileException ex) {
                    Logger.getLogger(mainUI.class.getName()).log(Level.SEVERE, null, ex);
                }
        }

}