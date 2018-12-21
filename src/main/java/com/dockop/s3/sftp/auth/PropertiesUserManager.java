/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.dockop.s3.sftp.auth;

import com.dockop.s3.sftp.common.SFTPAuthException;
import com.dockop.s3.sftp.common.SFTPServerException;
import com.dockop.s3.sftp.utils.BaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

public class PropertiesUserManager implements UserManager {

    private final Logger LOG = LoggerFactory
            .getLogger(PropertiesUserManager.class);

    private final static String PREFIX = "sftp.user.";

    private BaseProperties userDataProp;

    private File userDataFile;

    private URL userUrl;

    private PasswordEncryptor passwordEncryptor;

    public PropertiesUserManager(PasswordEncryptor passwordEncryptor,
                                 File userDataFile) {
        this.passwordEncryptor = passwordEncryptor;
        loadFromFile(userDataFile);
    }

    public PropertiesUserManager(PasswordEncryptor passwordEncryptor,
                                 InputStream is) {
        this.passwordEncryptor = passwordEncryptor;
        loadFromInputStream(is);
    }

    public PropertiesUserManager(PasswordEncryptor passwordEncryptor,
                                 URL userDataPath) {
        this.passwordEncryptor = passwordEncryptor;
        loadFromUrl(userDataPath);
    }

    private void loadFromFile(File userDataFile) {
        try {
            userDataProp = new BaseProperties();

            if (userDataFile != null) {
                LOG.debug("File configured, will try loading");

                if (userDataFile.exists()) {
                    this.userDataFile = userDataFile;

                    LOG.debug("File found on file system");
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(userDataFile);
                        userDataProp.load(fis);
                    } finally {
                        if (fis != null) {
                            fis.close();
                        }
                    }
                } else {
                    // try loading it from the classpath
                    LOG.debug("File not found on file system, try loading from classpath");

                    InputStream is = getClass().getClassLoader()
                            .getResourceAsStream(userDataFile.getPath());

                    if (is != null) {
                        try {
                            userDataProp.load(is);
                        } finally {
                            if (is != null) {
                                is.close();
                            }
                        }
                    } else {
                        throw new SFTPServerException(
                                "User data file specified but could not be located, "
                                        + "neither on the file system or in the classpath: "
                                        + userDataFile.getPath());
                    }
                }
            }
        } catch (IOException e) {
            throw new SFTPServerException(
                    "Error loading user data file : " + userDataFile, e);
        }
    }

    private void loadFromInputStream(InputStream is) {
        try {
            userDataProp = new BaseProperties();
            try {
                userDataProp.load(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new SFTPServerException(
                    "Error loading user data" , e);
        }
    }

    private void loadFromUrl(URL userDataPath) {
        try {
            userDataProp = new BaseProperties();

            if (userDataPath != null) {
                LOG.debug("URL configured, will try loading");

                userUrl = userDataPath;
                InputStream is = null;

                is = userDataPath.openStream();

                try {
                    userDataProp.load(is);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } catch (IOException e) {
            throw new SFTPServerException(
                    "Error loading user data resource : " + userDataPath, e);
        }
    }

    /**
     * Reloads the contents of the user.properties file. This allows any manual modifications to the file to be recognised by the running server.
     */
    public void refresh() {
        synchronized (userDataProp) {
            if (userDataFile != null) {
                LOG.debug("Refreshing user manager using file: "
                        + userDataFile.getAbsolutePath());
                loadFromFile(userDataFile);

            } else {
                //file is null, must have been created using URL
                LOG.debug("Refreshing user manager using URL: "
                        + userUrl.toString());
                loadFromUrl(userUrl);
            }
        }
    }

    /**
     * Retrive the file backing this user manager
     *
     * @return The file
     */
    public File getFile() {
        return userDataFile;
    }

    /**
     * Save user data. Store the properties.
     */
    public synchronized void save(User usr) throws SFTPServerException {
        // null value check
        if (usr.getName() == null) {
            throw new NullPointerException("User name is null.");
        }
        String thisPrefix = PREFIX + usr.getName() + '.';

        // set other properties
        userDataProp.setProperty(thisPrefix + ATTR_PASSWORD, getPassword(usr));

        String home = usr.getHomeDirectory();
        if (home == null) {
            home = "/";
        }
        userDataProp.setProperty(thisPrefix + ATTR_HOME, home);
        try {
            saveUserData();
        } catch (IOException e) {
            throw new SFTPServerException("io exception", e);
        }
    }

    /**
     * @throws SFTPServerException
     */
    private void saveUserData() throws SFTPServerException, IOException {
        if (userDataFile == null) {
            return;
        }

        File dir = userDataFile.getAbsoluteFile().getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            String dirName = dir.getAbsolutePath();
            throw new SFTPServerException(
                    "Cannot create directory for user data file : " + dirName);
        }

        // save user data
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(userDataFile);
            userDataProp.store(fos, "Generated file - don't edit (please)");
        } catch (IOException ex) {
            LOG.error("Failed saving user data", ex);
            throw new SFTPServerException("Failed saving user data", ex);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * Delete an user. Removes all this user entries from the properties. After
     * removing the corresponding from the properties, save the data.
     */
    public void delete(String usrName) throws SFTPServerException {
        // remove entries from properties
        String thisPrefix = PREFIX + usrName + '.';
        Enumeration<?> propNames = userDataProp.propertyNames();
        ArrayList<String> remKeys = new ArrayList<String>();
        while (propNames.hasMoreElements()) {
            String thisKey = propNames.nextElement().toString();
            if (thisKey.startsWith(thisPrefix)) {
                remKeys.add(thisKey);
            }
        }
        Iterator<String> remKeysIt = remKeys.iterator();
        while (remKeysIt.hasNext()) {
            userDataProp.remove(remKeysIt.next());
        }

        try {
            saveUserData();
        } catch (IOException e) {
            throw new SFTPServerException("io exception", e);
        }
    }

    /**
     * Get user password. Returns the encrypted value.
     * <p/>
     * <pre>
     * If the password value is not null
     *    password = new password
     * else
     *   if user does exist
     *     password = old password
     *   else
     *     password = &quot;&quot;
     * </pre>
     */
    private String getPassword(User usr) {
        String name = usr.getName();
        String password = usr.getPassword();

        if (password != null) {
            password = getPasswordEncryptor().encrypt(password);
        } else {
            String blankPassword = getPasswordEncryptor().encrypt("");

            if (doesExist(name)) {
                String key = PREFIX + name + '.' + ATTR_PASSWORD;
                password = userDataProp.getProperty(key, blankPassword);
            } else {
                password = blankPassword;
            }
        }
        return password;
    }

    /**
     * Get all user names.
     */
    public String[] getAllUserNames() {
        // get all user names
        String suffix = '.' + ATTR_HOME;
        ArrayList<String> ulst = new ArrayList<String>();
        Enumeration<?> allKeys = userDataProp.propertyNames();
        int prefixlen = PREFIX.length();
        int suffixlen = suffix.length();
        while (allKeys.hasMoreElements()) {
            String key = (String) allKeys.nextElement();
            if (key.endsWith(suffix)) {
                String name = key.substring(prefixlen);
                int endIndex = name.length() - suffixlen;
                name = name.substring(0, endIndex);
                ulst.add(name);
            }
        }

        Collections.sort(ulst);
        return ulst.toArray(new String[0]);
    }

    /**
     * Load user data.
     */
    public User getUserByName(String userName) {
        if (!doesExist(userName)) {
            return null;
        }

        String baseKey = PREFIX + userName + '.';
        User user = new User();
        user.setName(userName);
        user.setHomeDirectory(userDataProp.getProperty(baseKey + ATTR_HOME, "/"));
        user.setBucket(userDataProp.getProperty(baseKey + ATTR_BUCKET));
        user.setAccessKey(userDataProp.getProperty(baseKey + ATTR_ACCESS_KEY));
        user.setSecretKey(userDataProp.getProperty(baseKey + ATTR_SECRET_KEY));

        return user;
    }

    /**
     * User existance check
     */
    public boolean doesExist(String name) {
        String key = PREFIX + name + '.' + ATTR_HOME;
        return userDataProp.containsKey(key);
    }

    /**
     * User authenticate method
     */
    public User authenticate(Authentication authentication)
            throws SFTPAuthException {
        if (authentication instanceof UsernamePasswordAuthentication) {
            UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) authentication;

            String user = upauth.getUsername();
            String password = upauth.getPassword();

            if (user == null) {
                throw new SFTPAuthException("Authentication failed");
            }

            if (password == null) {
                password = "";
            }

            String storedPassword = userDataProp.getProperty(PREFIX + user
                    + '.' + ATTR_PASSWORD);

            if (storedPassword == null) {
                // user does not exist
                throw new SFTPAuthException("Authentication failed");
            }

            if (getPasswordEncryptor().matches(password, storedPassword)) {
                return getUserByName(user);
            } else {
                throw new SFTPAuthException("Authentication failed");
            }

        } else {
            throw new IllegalArgumentException(
                    "Authentication not supported by this user manager");
        }
    }

    public synchronized void dispose() {
        if (userDataProp != null) {
            userDataProp.clear();
            userDataProp = null;
        }
    }


    @Override
    public PasswordEncryptor getPasswordEncryptor() {
        return this.passwordEncryptor;
    }
}
