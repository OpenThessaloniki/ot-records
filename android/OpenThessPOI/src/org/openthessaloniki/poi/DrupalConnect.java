package org.openthessaloniki.poi;

import java.io.IOException;
import java.net.MalformedURLException;

import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;


/**
 * Class, that performs authentication and communications with Drupal. 
 * @author Moskvichev Andrey V.
 *
 */
public class DrupalConnect {
	/** Base url of Drupal installation */
	final static private String URLBASE = "http://aws.hypest.com/openthess";
	
	/** XML-RPC url */
	final static private String XMLRPC = URLBASE + "/?q=androidrpc";
	
	/** Singleton instance */
	static private DrupalConnect instance;

	private String csrf_token;

	// session params, returned by user.login, and then passed in all 
	// subsequent calls as cookie
	private String sessid;
	private String session_name;
	
	private DrupalConnect() {
	}
	
	/**
	 * Perform authentication.
	 * @param username user name
	 * @param password password
	 * @throws IOException
	 * @throws XmlRpcException
	 * @throws XmlRpcFault
	 */
	public void login(String username, String password) throws IOException, XmlRpcException, XmlRpcFault {
		if (isAuthenticated())
			logout();
		
		XmlRpcClient xmlrpc = new XmlRpcClient(XMLRPC, false);

		// get the token
		XmlRpcStruct tokenRes = (XmlRpcStruct) xmlrpc.invoke("user.token", new Object[] {});
		xmlrpc.setRequestProperty("X-CSRF-Token", tokenRes.getString("token"));

		// login using the token
		XmlRpcStruct res = (XmlRpcStruct) xmlrpc.invoke("user.login", new Object[] { username, password });

		// get the the cookie
		sessid = res.getString("sessid");
		session_name = res.getString("session_name");

		xmlrpc.setRequestProperty("Cookie", getSessionCookieString());
		tokenRes = (XmlRpcStruct) xmlrpc.invoke("user.token", new Object[] {});
		csrf_token = tokenRes.getString("token");
	}
	
	/**
	 * Close session.
	 * @throws MalformedURLException
	 * @throws XmlRpcException
	 * @throws XmlRpcFault
	 */
	public void logout() throws MalformedURLException, XmlRpcException, XmlRpcFault {
		if (!isAuthenticated())
			return ;
					
		try {
			// create xml-rpc client
			XmlRpcClient xmlrpc = new XmlRpcClient(XMLRPC, false);
			xmlrpc.setRequestProperty("X-CSRF-Token", csrf_token);
			// set session cookie		
			xmlrpc.setRequestProperty("Cookie", getSessionCookieString());

			// remote call
			xmlrpc.invoke("user.logout", new Object[] { });
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		sessid = null;
		session_name = null;
	}

	/**
	 * Checks if user is authenticated.
	 * @return <code>true</code> if authenticated
	 */
	public boolean isAuthenticated() {
		if (sessid == null)
			return false;
		return true;
	}

	/**
	 * Posts article.
	 * @param title article title
	 * @param body article body
	 * @return article node identifier
	 * @throws IOException
	 * @throws XmlRpcException
	 * @throws XmlRpcFault
	 */
	@SuppressWarnings("unchecked")
	public int postArticle(String title, String body) throws IOException, XmlRpcException, XmlRpcFault {
		// check if user is authenticated
		if (!isAuthenticated()) {
			throw new IllegalStateException("Session is not open.");
		}
		
		// create xml-rpc client
		XmlRpcClient xmlrpc = new XmlRpcClient(XMLRPC, false);
		xmlrpc.setRequestProperty("X-CSRF-Token", csrf_token);
		// set session cookie
		xmlrpc.setRequestProperty("Cookie", getSessionCookieString());
		
		// set page values
		XmlRpcStruct params = new XmlRpcStruct();
		params.put("type", "article");
		params.put("title", title);
		params.put("body", body);
		
		// remote call
		XmlRpcStruct res = (XmlRpcStruct) xmlrpc.invoke("node.create", new Object[] { params });
		
		// get page nid and return it 
		return Integer.parseInt(res.get("nid").toString());
	}
	
	/**
	 * Posts page.
	 * @param title page title
	 * @param body page body
	 * @return page node identifier
	 * @throws IOException
	 * @throws XmlRpcException
	 * @throws XmlRpcFault
	 */
	@SuppressWarnings("unchecked")
	public int postPage(String title, String body) throws IOException, XmlRpcException, XmlRpcFault {
		// check if user is authenticated
		if (!isAuthenticated()) {
			throw new IllegalStateException("Session is not open.");
		}
		
		// create xml-rpc client
		XmlRpcClient xmlrpc = new XmlRpcClient(XMLRPC, false);
		xmlrpc.setRequestProperty("X-CSRF-Token", csrf_token);
		// set session cookie
		xmlrpc.setRequestProperty("Cookie", getSessionCookieString());
		
		// set page values
		XmlRpcStruct params = new XmlRpcStruct();
		params.put("type", "page");
		params.put("title", title);
		params.put("body", body);
		
		// remote call
		XmlRpcStruct res = (XmlRpcStruct) xmlrpc.invoke("node.create", new Object[] { params });
		
		// get page nid and return it 
		return Integer.parseInt(res.get("nid").toString());
	}
	
	/**
	 * Delete page.
	 * @param nid page node identifier
	 * @throws IOException
	 * @throws XmlRpcException
	 * @throws XmlRpcFault
	 */
	@SuppressWarnings("unchecked")
	public boolean deletePage(int nid) throws IOException, XmlRpcException, XmlRpcFault {
		// check if user is authenticated
		if (!isAuthenticated()) {
			throw new IllegalStateException("Session is not open.");
		}
		
		// create xml-rpc client
		XmlRpcClient xmlrpc = new XmlRpcClient(XMLRPC, false);
		xmlrpc.setRequestProperty("X-CSRF-Token", csrf_token);
		// set session cookie
		xmlrpc.setRequestProperty("Cookie", getSessionCookieString());
		
		// page params: nid
		XmlRpcStruct params = new XmlRpcStruct();
		params.put("nid", ""+nid);
		
		// node.delete return boolean indicating, whether node is removed or not
		return (Boolean) xmlrpc.invoke("node.delete", new Object[] { params });
	}
	

	private String getSessionCookieString() {
		if (sessid == null || session_name == null)
			return null;
		
		return session_name+"="+sessid;
	}

	public static DrupalConnect getInstance() {
		if (instance == null)
			instance = new DrupalConnect();
		return instance;
	}
}
