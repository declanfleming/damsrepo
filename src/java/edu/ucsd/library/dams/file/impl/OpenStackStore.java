package edu.ucsd.library.dams.file.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import javax.activation.FileDataSource;

import org.apache.log4j.Logger;

import edu.ucsd.library.dams.file.FileStore;
import edu.ucsd.library.dams.file.FileStoreException;
import edu.ucsd.library.dams.file.FileStoreUtil;

/**
 * FileStore implementation for OpenStack Storage API.
 * @see util/SwiftClient
 * @author escowles@ucsd.edu
**/
public class OpenStackStore implements FileStore
{
	private static Logger log = Logger.getLogger(OpenStackStore.class);

	private SwiftClient client = null;
	private String orgCode = null;
	private int retryCount = 0;

/*****************************************************************************/
/********** Constructors *****************************************************/
/*****************************************************************************/

	/**
	 * Create an OpenStackStore object, getting parameters from a Properties
	 *   object.
	 * @param props Properties object containing the following properties:
	 *  authUser, authToken, authURL, orgCode, retryCount.
	**/
	public OpenStackStore( Properties props ) throws FileStoreException
	{
		try
		{
			PrintStream out = null;
			String debug = props.getProperty("debug");
			if ( debug != null && !debug.equals("") && !debug.equals("false") )
			{
				out = System.out;
			}
			client = new SwiftClient( props, out );
			orgCode = props.getProperty("orgCode");
			String retryCountString = props.getProperty("retryCount");
			try { retryCount = Integer.parseInt(retryCountString); }
			catch ( Exception ex )
			{
				retryCount = 0;
				System.out.println(
					"Unable to parse retryCount: '"+ retryCountString
					+ "', disabling retries"
				);
			}
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
	}


/*****************************************************************************/
/********** FileStore impl ***************************************************/
/*****************************************************************************/

	public String[] listComponents( String objectID ) throws FileStoreException
	{
		String[] fileArr = null;
		try
		{
			// return empty list if the container doesn't exist
			if ( !client.exists(cn(orgCode,objectID),null) )
			{
				return new String[]{ };
			}

			// list files
			List<String> files = client.listObjects(
				null, cn(orgCode,objectID), path(objectID)
			);

			// exclude manifest.txt
			String path = path(objectID);
			String stem = stem(orgCode,objectID);
			if ( path != null ) { stem = path + "/" + stem; }
			HashSet<String> files2 = new HashSet<String>();
			for ( int i = 0; i < files.size(); i++ )
			{
				String fn = files.get(i);
				if ( fn.startsWith(stem) )
				{
					fn = fn.substring(stem.length());
				}
				if ( !fn.endsWith("/") )
				{
					if ( fn.indexOf("-") > 0 )
					{
						fn = fn.substring(0,fn.indexOf("-"));
					}
					files2.add( fn );
				}
			}

			// make array
			fileArr = files2.toArray( new String[files2.size()] );
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
		return fileArr;
	}
	public String[] listFiles( String objectID, String componentID )
		throws FileStoreException
	{
		String[] fileArr = null;
		try
		{
			// return empty list if the container doesn't exist
			if ( !client.exists(cn(orgCode,objectID),null) )
			{
				return new String[]{ };
			}

			// list files
			List<String> files = client.listObjects(
				null, cn(orgCode,objectID), path(objectID)
			);

			// exclude manifest.txt
			String path = path(objectID);
			String stem = stem(orgCode,objectID,componentID);
			if ( path != null ) { stem = path + "/" + stem; }
			List<String> files2 = new ArrayList<String>();
			for ( int i = 0; i < files.size(); i++ )
			{
				String fn = files.get(i);
				String orig = fn;
				if ( fn.startsWith(stem) )
				{
					fn = fn.substring(stem.length());
					if ( !fn.equals("manifest.txt") && !fn.endsWith("/") )
					{
						files2.add( fn );
					}
				}
			}

			// make array
			fileArr = files2.toArray( new String[files2.size()] );
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
		return fileArr;
	}
	public boolean exists( String objectID, String componentID, String fileID )
		throws FileStoreException
	{
		boolean exists = false;
		try
		{
			exists = client.exists( cn(orgCode,objectID), fn(orgCode,objectID,componentID,fileID) );
		}
		catch ( IOException ex )
		{
			// throw exceptions
			throw new FileStoreException(ex);
		}
		return exists;
	}
	public Map<String,String> meta( String objectID, String componentID, String fileID )
		throws FileStoreException
	{
		Map<String,String> md = null;
		try
		{
			// retrieve metadata for a file
			md = client.stat(
				cn(orgCode,objectID), fn(orgCode,objectID,componentID,fileID)
			);
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
		return md;
	}
	public long length( String objectID, String componentID, String fileID )
		throws FileStoreException
	{
		// retrieve metadata for a file
		Map<String,String> md = meta(objectID,componentID,fileID);

		// parse size field
		return Long.parseLong( md.get("Content-Length") );
	}
	public byte[] read( String objectID, String componentID, String fileID )
		throws FileStoreException
	{
		try
		{
			InputStream in = client.read( null, cn(orgCode,objectID), fn(orgCode,objectID,componentID,fileID) );
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			FileStoreUtil.copy( in, out );
			in.close();
			return out.toByteArray();
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
	}
	public byte[] readManifest( String objectID ) throws FileStoreException
	{
		return read( objectID, null, "manifest.txt" );
	}
	public void read( String objectID, String componentID, String fileID, OutputStream out )
		throws FileStoreException
	{
		InputStream in = null;
		try
		{
			in = client.read( null, cn(orgCode,objectID), fn(orgCode,objectID,componentID,fileID) );
			FileStoreUtil.copy( in, out );
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
		finally
		{
			try { in.close(); } catch ( Exception ex2 ) { }
		}
	}
	public InputStream getInputStream( String objectID, String componentID, String fileID )
		throws FileStoreException
	{
		InputStream in = null;
		try
		{
			in = client.read( null, cn(orgCode,objectID), fn(orgCode,objectID,componentID,fileID) );
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
		return in;
	}
	public void write( String objectID, String componentID, String fileID,
		byte[] data ) throws FileStoreException
	{
		write( objectID, componentID, fileID, new ByteArrayInputStream(data) );
	}
	public void writeManifest( String objectID, byte[] data )
		throws FileStoreException
	{
		write( objectID, null, "manifest.txt", data );
	}
	public void write( String objectID, String componentID, String fileID,
		File f ) throws FileStoreException
	{
		try
		{
			long len = f.length();
			if ( len > client.segmentSize() )
			{
				// create container if it doesn't already exist
				if ( !client.exists(cn(orgCode,objectID),null) )
				{
					client.createContainer(cn(orgCode,objectID));
				}

				// upload in segments
				int status = 0;
				for ( int i = 0; i <= retryCount; i++ )
				{
					try
					{
						status = client.uploadSegmented(
							cn(orgCode,objectID),
							fn(orgCode,objectID,componentID,fileID),
							new FileInputStream(f), len
						);
					}
					catch ( Exception ex )
					{
						log.warn("Error uploading file",ex);
						if ( i < retryCount )
						{
							log.warn("Retrying upload (" + i + ")...");
						}
					}
				}
			}
			else
			{
				write( objectID, componentID, fileID, new FileInputStream(f) );
			}
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
	}
	public void write( String objectID, String componentID, String fileID,
		InputStream in ) throws FileStoreException
	{
		try
		{
			// create container if it doesn't already exist
			if ( !client.exists(cn(orgCode,objectID),null) )
			{
				client.createContainer(cn(orgCode,objectID));
			}

			// don't know how large the source is, just try to upload the
			// whole object and throw an error if the 5GB limit is reached
			client.upload(
				cn(orgCode,objectID),
				fn(orgCode,objectID,componentID,fileID),
				in, -1
			);
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
	}
	public void close() throws FileStoreException
	{
		client = null;
	}
	public void copy( String srcObjID, String srcCompID, String srcFileID,
		String dstObjID, String dstCompID, String dstFileID )
		throws FileStoreException
	{
		try
		{
			// make sure destination container exists
			if ( !client.exists(dstObjID,null) )
			{
				client.createContainer(dstObjID);
			}
			client.copy(
				cn(orgCode,srcObjID), fn(orgCode,srcObjID,srcCompID,srcFileID),
				cn(orgCode,dstObjID), fn(orgCode,dstObjID,dstCompID,dstFileID)
			);
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
	}
	public void trash( String objectID, String componentID, String fileID )
		throws FileStoreException
	{
		try
		{
			// make sure trash container exists
			if ( !client.exists("trash",null) )
			{
				client.createContainer("trash");
			}
			client.copy( cn(orgCode,objectID), fn(orgCode,objectID,componentID,fileID), "trash", cn(orgCode,objectID) + "/" + fn(orgCode,objectID,componentID,fileID) );
			client.delete( cn(orgCode,objectID), fn(orgCode,objectID,componentID,fileID) );
		}
		catch ( IOException ex )
		{
			throw new FileStoreException(ex);
		}
	}
	public String orgCode() { return orgCode; }
	public String getPath( String objectID, String componentID, String fileID )
	{
		return cn(orgCode,objectID) + "/" + fn(orgCode,objectID,componentID,fileID);
	}

/*****************************************************************************/
/********** Internal file-naming conventions *********************************/
/*****************************************************************************/
	protected static String cn( String orgCode, String objectID )
	{
		return objectID.substring(0,2);
	}
	protected static String stem( String orgCode, String objectID )
	{
		String stem = "";
		if ( orgCode != null && !orgCode.equals("") )
		{
			stem = orgCode + "-";
		}
		stem += objectID + "-";
		return stem;
	}
	protected static String stem( String orgCode, String objectID,
		String componentID )
	{
		String stem = stem( orgCode, objectID );
		if ( componentID != null ) { stem += componentID + "-"; }
		else { stem += "0-"; }
		return stem;
	}
	protected static String path( String objectID )
	{
		return objectID;
	}
	protected static String fn( String orgCode, String objectID,
		String componentID, String fileID )
	{
		String fn;
		if ( fileID == null )
		{
			return null;
		}

		String stem = stem(orgCode,objectID,componentID);
		if ( !fileID.startsWith(stem) )
		{
			fn = objectID + "/" + stem + fileID;
		}
		else
		{
			fn = objectID + "/" + fileID;
		}
		return fn;
	}
}
