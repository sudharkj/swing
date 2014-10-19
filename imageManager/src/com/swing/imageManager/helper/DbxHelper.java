/**
 * 
 */
package com.swing.imageManager.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.json.JsonReader;

/**
 * @author Ravi
 * 
 */
public class DbxHelper {

	final static Logger LOGGER = LogManager.getLogger(DbxHelper.class);

	public final static String CLASS_NAME = "DbxHelper";
	public final static String HEADER = "In " + CLASS_NAME + ":\n";

	public DbxHelper() {
		File authInfoFile = new File(Helper.USER_AUTH_TOKEN_FILE_NAME);
		if (!authInfoFile.exists() || authInfoFile.isDirectory())
			getAuthInfo();

		new Thread(new DbxUploader(), DbxUploader.CLASS_NAME).start();
	}

	public void getAuthInfo() {
		DbxAppInfo appInfo = null;
		try {
			appInfo = DbxAppInfo.Reader
					.readFromFile(Helper.APP_AUTH_DETAILS_FILE_NAME);
		} catch (JsonReader.FileLoadException e) {
			LOGGER.info(HEADER + "Error reading <app-info-file>: "
					+ e.getMessage());
		}

		// Run through Dropbox API authorization process
		String userLocale = Locale.getDefault().toString();
		DbxRequestConfig requestConfig = new DbxRequestConfig(CLASS_NAME,
				userLocale);
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(requestConfig,
				appInfo);

		String authorizeUrl = webAuth.start();
		LOGGER.info("1. Go to " + authorizeUrl
				+ "\n2. Click \"Allow\" (you might have to log in first).\n"
				+ "3. Copy the authorization code.\n"
				+ "Enter the authorization code here: ");

		String code = null;
		try {
			code = new BufferedReader(new InputStreamReader(System.in))
					.readLine();
		} catch (IOException e) {
			LOGGER.info(HEADER + "Error reading <code>: " + e.getMessage());
		}
		if (code == null) {
			LOGGER.info(HEADER + "Obtained <code>: null");
		}
		code = code.trim();

		DbxAuthFinish authFinish = null;
		try {
			authFinish = webAuth.finish(code);
		} catch (DbxException ex) {
			LOGGER.info(HEADER + "Error in DbxWebAuth.start: "
					+ ex.getMessage());
		}

		LOGGER.info("Authorization complete.\n" + "- User ID: "
				+ authFinish.userId + "\n- Access Token: "
				+ authFinish.accessToken);

		// Save auth information to output file.
		DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.accessToken,
				appInfo.host);

		try {
			DbxAuthInfo.Writer.writeToFile(authInfo,
					Helper.USER_AUTH_TOKEN_FILE_NAME);
			LOGGER.info("Saved authorization information to \""
					+ Helper.USER_AUTH_TOKEN_FILE_NAME + "\".");
		} catch (IOException ex) {
			LOGGER.info(HEADER + "Error saving to <auth-file-out>: "
					+ ex.getMessage() + "\nDumping to stderr instead: "
					+ authInfo.toString());
		}
	}

	public static DbxClient getDbxClient() {
		// Read auth info file.
		DbxAuthInfo authInfo = null;
		try {
			authInfo = DbxAuthInfo.Reader
					.readFromFile(Helper.USER_AUTH_TOKEN_FILE_NAME);
		} catch (JsonReader.FileLoadException e) {
			LOGGER.info(HEADER + "Error reading <user-auth-token-file>: "
					+ e.getMessage());
		}

		// Create a DbxClient, which is what you use to make API calls.
		String userLocale = Locale.getDefault().toString();
		DbxRequestConfig requestConfig = new DbxRequestConfig(CLASS_NAME,
				userLocale);
		return new DbxClient(requestConfig, authInfo.accessToken, authInfo.host);
	}

}
