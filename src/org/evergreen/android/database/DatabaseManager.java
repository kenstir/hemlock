
package org.evergreen.android.database;

import java.util.HashMap;
import java.util.logging.Logger;

import org.androwrapee.db.DefaultDAO;
import org.androwrapee.db.DefaultDatabaseHelper;
import org.androwrapee.db.IllegalClassStructureException;
import org.androwrapee.db.ReflectionManager;
import org.evergreen.android.services.NotificationAlert;

import android.content.Context;
import android.util.Log;

/**
 * The Class DatabaseDefaults.
 */
public class DatabaseManager {

	public static String TAG = "DatabaseManager";
	
	/** The DATABASE NAME. */
	public static final String DATABASE_NAME = "evergreen.db";

	/** The DATABASE VERSION. */
	public static final int DATABASE_VERSION = 1;

	/** The db helper. */
	private static DefaultDatabaseHelper dbHelper = null;

	/** The singleton reflection managers map. */
	@SuppressWarnings("rawtypes")
	private static HashMap<Class, ReflectionManager> rmMap = new HashMap<Class, ReflectionManager>();

	@SuppressWarnings("rawtypes")
	private static HashMap<Class, DefaultDAO> daoMap = new HashMap<Class, DefaultDAO>();

	/**
	 * Gets the Singleton database helper.
	 * 
	 * @return the dB helper
	 */
	public static DefaultDatabaseHelper getDBHelper(Context context) {
		if (dbHelper == null)
			dbHelper = new DefaultDatabaseHelper(context, DATABASE_NAME, DATABASE_VERSION, new Class[] { NotificationAlert.class}, new String[] {
					NotificationAlert.tableName});
		return dbHelper;
	}

	/**
	 * Gets a singleton instance of a reflection manager corresponding to a class.
	 * 
	 * @param cls the class
	 * @return the reflection manager instance
	 */
	public static <T> ReflectionManager getReflectionManagerInstance(Class<T> cls) {
		if (rmMap.containsKey(cls))
			return rmMap.get(cls);
		try {
			ReflectionManager rm = new ReflectionManager(cls);
			rmMap.put(cls, rm);
			return rm;
		} catch (IllegalClassStructureException ex) {
			ex.printStackTrace();
			Log.d(TAG, "Illegal Class Structure for class " + cls + ": " + ex.getMessage());
			return null;
		}
	}

	/**
	 * Gets a singleton instance of a DefaultDAO object corresponding to a class.
	 * 
	 * @param <T> the generic type
	 * @param cls the class
	 * @param tableName the table name
	 * @return the DAO instance
	 */
	@SuppressWarnings("unchecked")
	public static synchronized <T> DefaultDAO<T> getDAOInstance(Context context, Class<T> cls, String tableName) {
		if (daoMap.containsKey(cls))
			return daoMap.get(cls);
		DefaultDAO<T> dao = new DefaultDAO<T>(cls, DatabaseManager.getDBHelper(context),
				DatabaseManager.getReflectionManagerInstance(cls), tableName);
		daoMap.put(cls, dao);
		return dao;
	}
}
