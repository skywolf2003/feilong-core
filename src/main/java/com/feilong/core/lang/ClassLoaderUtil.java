/*
 * Copyright (C) 2008 feilong (venusdrogon@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.feilong.core.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilong.core.io.UncheckedIOException;
import com.feilong.core.tools.json.JsonUtil;

/**
 * 根据类的class文件位置来定位的方法.
 * 
 * @author <a href="mailto:venusdrogon@163.com">金鑫</a>
 * @version 1.0 2011-4-27 上午12:40:08
 * @since 1.0.0
 */
public final class ClassLoaderUtil{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(ClassLoaderUtil.class);

    /** Don't let anyone instantiate this class. */
    private ClassLoaderUtil(){
        //AssertionError不是必须的. 但它可以避免不小心在类的内部调用构造器. 保证该类在任何情况下都不会被实例化.
        //see 《Effective Java》 2nd
        throw new AssertionError("No " + getClass().getName() + " instances for you!");
    }

    /**
     * 查找具有给定名称的资源.
     * <p>
     * "",表示classes 的根目录
     * </p>
     * e.q:<br>
     * 
     * <blockquote>
     * <table border="1" cellspacing="0" cellpadding="4">
     * <tr style="background-color:#ccccff">
     * <th align="left"></th>
     * <th align="left">(maven)测试</th>
     * <th align="left">在web环境中,(即使打成jar的情形)</th>
     * </tr>
     * <tr valign="top">
     * <td>{@code getResource("")}</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-common/target/test-classes/</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-spring-test-2.5/src/main/webapp/WEB-INF/classes/</td>
     * </tr>
     * <tr valign="top" style="background-color:#eeeeff">
     * <td>{@code getResource("com")}</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-common/target/test-classes/com</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-spring-test-2.5/src/main/webapp/WEB-INF/classes/com/</td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @param resourceName
     *            the resource name
     * @return 查找具有给定名称的资源
     */
    public static URL getResource(String resourceName){
        ClassLoader classLoader = getClassLoaderByClass(ClassLoaderUtil.class);
        return getResource(classLoader, resourceName);
    }

    /**
     * 获得 项目的 classpath,及classes编译的根目录.
     * 
     * @return 获得 项目的 classpath
     * @see #getResource(String)
     */
    public static URL getClassPath(){
        ClassLoader classLoader = getClassLoaderByClass(ClassLoaderUtil.class);
        return getClassPath(classLoader);
    }

    /**
     * 获得 class path.
     *
     * @param classLoader
     *            the class loader
     * @return the class path
     * @see #getResource(ClassLoader, String)
     */
    public static URL getClassPath(ClassLoader classLoader){
        return getResource(classLoader, "");
    }

    // *****************************************************
    /**
     * This is a convenience method to load a resource as a stream. <br>
     * The algorithm used to find the resource is given in getResource()
     * 
     * @param resourceName
     *            The name of the resource to load
     * @param callingClass
     *            The Class object of the calling object
     * @return the resource as stream
     * @see #getResource(String, Class)
     */
    public static InputStream getResourceAsStream(String resourceName,Class<?> callingClass){
        URL url = getResource(resourceName, callingClass);
        try{
            return (url != null) ? url.openStream() : null;
        }catch (IOException e){
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Load a given resource.
     * <p>
     * This method will try to load the resource using the following methods (in order):
     * </p>
     * <ul>
     * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     * 
     * @param resourceName
     *            The name of the resource to load
     * @param callingClass
     *            The Class object of the calling object
     * @return the resource
     */
    public static URL getResource(String resourceName,Class<?> callingClass){
        ClassLoader classLoader = getClassLoaderByCurrentThread();
        URL url = classLoader.getResource(resourceName);
        if (url == null){
            log.warn(
                            "In ClassLoader:[{}],not found the resourceName:[{}]",
                            JsonUtil.format(getClassLoaderInfoMapForLog(classLoader)),
                            resourceName);

            classLoader = getClassLoaderByClass(ClassLoaderUtil.class);
            url = getResource(classLoader, resourceName);

            if (url == null){
                log.warn(
                                "In ClassLoader:[{}],not found the resourceName:[{}]",
                                JsonUtil.format(getClassLoaderInfoMapForLog(classLoader)),
                                resourceName);
                classLoader = getClassLoaderByClass(callingClass);
                url = getResource(classLoader, resourceName);
            }
        }
        if (url == null){
            log.warn("resourceName:[{}] in all ClassLoader not found", resourceName);
        }else{
            log.debug(
                            "found the resourceName:[{}],In ClassLoader :[{}] ",
                            resourceName,
                            JsonUtil.format(getClassLoaderInfoMapForLog(classLoader)));
        }
        return url;
    }

    /**
     * 查找具有给定名称的资源.
     * <p>
     * "",表示classes 的根目录
     * </p>
     * e.q:<br>
     * 
     * <blockquote>
     * <table border="1" cellspacing="0" cellpadding="4">
     * <tr style="background-color:#ccccff">
     * <th align="left"></th>
     * <th align="left">(maven)测试</th>
     * <th align="left">在web环境中,(即使打成jar的情形)</th>
     * </tr>
     * <tr valign="top">
     * <td>{@code getResource("")}</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-common/target/test-classes/</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-spring-test-2.5/src/main/webapp/WEB-INF/classes/</td>
     * </tr>
     * <tr valign="top" style="background-color:#eeeeff">
     * <td>{@code getResource("com")}</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-common/target/test-classes/com</td>
     * <td>file:/E:/Workspaces/feilong/feilong-platform/feilong-spring-test-2.5/src/main/webapp/WEB-INF/classes/com/</td>
     * </tr>
     * </table>
     * </blockquote>
     *
     * @param classLoader
     *            the class loader
     * @param resourceName
     *            the resource name
     * @return the resource
     * @since 1.2.1
     */
    public static URL getResource(ClassLoader classLoader,String resourceName){
        URL url = classLoader.getResource(resourceName);
        return url;
    }

    /**
     * Load resources.
     * 
     * @param resourceName
     *            the resource name
     * @param callingClass
     *            the calling class
     * @return the resources
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @see java.lang.ClassLoader#getResources(String)
     */
    public static Enumeration<URL> getResources(String resourceName,Class<?> callingClass) throws IOException{
        ClassLoader classLoader = getClassLoaderByCurrentThread();
        Enumeration<URL> urls = classLoader.getResources(resourceName);
        if (urls == null){
            classLoader = getClassLoaderByClass(ClassLoaderUtil.class);
            urls = classLoader.getResources(resourceName);
            if (urls == null){
                classLoader = getClassLoaderByClass(callingClass);
                urls = classLoader.getResources(resourceName);
            }
        }
        if (urls == null){
            log.warn("resourceName:[{}] in all ClassLoader not found!", resourceName);
        }else{
            log.debug(
                            "In ClassLoader :[{}] found the resourceName:[{}]",
                            JsonUtil.format(getClassLoaderInfoMapForLog(classLoader)),
                            resourceName);
        }
        return urls;
    }

    /**
     * Load a class with a given name.
     * <p>
     * It will try to load the class in the following order:
     * </p>
     * <ul>
     * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>Using the basic {@link Class#forName(java.lang.String) }
     * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     * 
     * @param className
     *            The name of the class to load
     * @param callingClass
     *            The Class object of the calling object
     * @return the class
     * @throws ClassNotFoundException
     *             If the class cannot be found anywhere.
     * @see java.lang.ClassLoader#loadClass(String)
     */
    public static Class<?> loadClass(String className,Class<?> callingClass) throws ClassNotFoundException{
        ClassLoader classLoader = null;
        try{
            classLoader = getClassLoaderByCurrentThread();
            return classLoader.loadClass(className);
        }catch (ClassNotFoundException e){
            try{
                return Class.forName(className);
            }catch (ClassNotFoundException ex){
                try{
                    classLoader = getClassLoaderByClass(ClassLoaderUtil.class);
                    return classLoader.loadClass(className);
                }catch (ClassNotFoundException exc){
                    classLoader = getClassLoaderByClass(callingClass);
                    return classLoader.loadClass(className);
                }
            }
        }
    }

    /**
     * 通过Thread.currentThread().getContextClassLoader() 获得ClassLoader
     * 
     * @return the class loader by current thread
     */
    public static ClassLoader getClassLoaderByCurrentThread(){
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (log.isDebugEnabled()){
            log.debug("Thread.currentThread().getContextClassLoader:{}", JsonUtil.format(getClassLoaderInfoMapForLog(classLoader)));
        }
        return classLoader;
    }

    /**
     * 通过类来获得 classLoader.
     * 
     * @param callingClass
     *            the calling class
     * @return the class loader by class
     * @see java.lang.Class#getClassLoader()
     */
    public static ClassLoader getClassLoaderByClass(Class<?> callingClass){
        ClassLoader classLoader = callingClass.getClassLoader();
        if (log.isDebugEnabled()){
            log.debug("{}.getClassLoader():{}", callingClass.getSimpleName(), JsonUtil.format(getClassLoaderInfoMapForLog(classLoader)));
        }
        return classLoader;
    }

    /**
     * 获得 class loader info map for log.
     *
     * @param classLoader
     *            the class loader
     * @return the class loader info map for log
     * @since 1.1.1
     */
    public static Map<String, Object> getClassLoaderInfoMapForLog(ClassLoader classLoader){
        Map<String, Object> classLoaderInfoMap = new LinkedHashMap<String, Object>();
        classLoaderInfoMap.put("classLoader", "" + classLoader);
        classLoaderInfoMap.put("classLoader[CanonicalName]", classLoader.getClass().getCanonicalName());
        classLoaderInfoMap.put("classLoader[Root Classpath]", "" + getClassPath(classLoader));
        return classLoaderInfoMap;
    }
}