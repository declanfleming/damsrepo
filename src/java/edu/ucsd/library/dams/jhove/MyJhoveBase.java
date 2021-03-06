/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2005 by the President and Fellows of Harvard College
 **********************************************************************/

package edu.ucsd.library.dams.jhove;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.XPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.OutputHandler;
import edu.harvard.hul.ois.jhove.handler.XmlHandler;

import org.portico.tool.jhove_1_1.PTC_ZipModule_1_1;
import org.portico.tool.jhove_1_1.PTC_GzipModule_1_1;

/**
 * The JHOVE engine, providing all base services necessary to build an
 * application.
 * 
 * More than one JhoveBase may be instantiated and process files in
 * concurrent threads.  Any one instance must not be multithreaded.
 */
public class MyJhoveBase extends JhoveBase {
	
	private static Logger log = Logger.getLogger(MyJhoveBase.class);

	//private MyJhoveBase _jebase = null;
	private static List<MyJhoveBase> myJhoves = new ArrayList<MyJhoveBase>();
	public static boolean shellMode = false;
	private static String jhoveconf = "conf/jhove.conf";
	private static String zipModelCommand = "unzip";
	private static String gzipModelCommand = "gzip";
	private static final String jhvname = "ETL-Jhove";
	private static final int [] jhvdate = {2005, 9, 25};
	private static final String jvhrel = "1.0";
	private static final String jvhrights = "Copyright 2004-2005 by JSTOR and " +
	"the President and Fellows of Harvard College. " +
	"Released under the GNU Lesser General Public License.";
	
	private static final String BYTESTREAM = "BYTESTREAM";
	private static final String[] BYTESTREAM_MODULE_EXTS = {".mov",".hierarchy",".wrl",".st"};
	private static final String _moduleNames[] = {"PDF-hul","ASCII-hul","GIF-hul","TIFF-hul","WAVE-hul","XML-hul","HTML-hul","ZIP-ptc","GZIP-ptc"};
	private static final String _fileExten[]   = {".pdf",".txt",".gif",".tif",".wav",".xml",".html",".zip",".gz"};
	private static HashMap _moduleMap;
	private static String ffmpegCommand = "ffmpeg";
	public static final String MEDIA_FILES = ".wav .mp3 .mov .mp4 .avi .png";


	private MyJhoveBase() throws Exception {
		super();
		initModuleMap();
	}
	
	public static synchronized MyJhoveBase getMyJhoveBase() throws Exception{
		MyJhoveBase jebase = null;
		if(myJhoves.size() == 0){
			if(jhoveconf == null || !new File(jhoveconf).exists()){
				jhoveconf = "dams/jhove.conf";
				if(!new File(jhoveconf).exists())
					throw new Exception("Configuration file was not found: " + jhoveconf);
			}
			String saxClass = MyJhoveBase.getSaxClassFromProperties();
			jebase = new MyJhoveBase();
			jebase.init(MyJhoveBase.jhoveconf, saxClass);
		}else
			jebase = myJhoves.remove(0);
		return jebase;
	}
	
	public static synchronized void returnMyJhoveBase(MyJhoveBase jebase) throws Exception{
		myJhoves.add(jebase);
	}
	//rdias - UCSD: my version of dispatch into order to take control of the 
	//output writer in dispatch
    /** Processes a file or directory, or outputs information.
     *  If <code>dirFileOrUri</code> is null, Does one of the following:
     *  <ul>
     *   <li>If module is non-null, provides information about the module.
     *   <li>Otherwise if <code>aboutHandler</code> is non-null,
     *       provides information about that handler.
     *   <li>If they're both null, provides information about the
     *       application.
     *  </ul>
     *  @param app          The App object for the application
     *  @param module       The module to be used
     *  @param aboutHandler If specified, the handler about which info is requested
     *  @param handler      The handler for processing the output
     *  @param outputFile   Name of the file to which output should go
     *  @param dirFileOrUri One or more file names or URI's to be analyzed
     */
    public void dispatch (App app, Module module, /* String moduleParam, */
				OutputHandler aboutHandler,
				OutputHandler handler, /*String handlerParam,*/
				PrintWriter output,
				String [] dirFileOrUri)
	throws Exception
    {
        super.resetAbort();
    	/* If no handler is specified, use the default TEXT handler. */
    	if (handler == null) {
    	    handler = (OutputHandler) super.getHandlerMap().get ("text");
    	}
    
    	handler.setApp    (app);
    	handler.setBase   (this);
    	handler.setWriter(output);
//    	handler.setWriter (makeWriter (_outputFile, _encoding));
    	//handler.param     (handlerParam);
    
    	handler.showHeader ();                /* Show handler header info. */
    
    	if (dirFileOrUri == null) {
            if (module != null) {             /* Show info about module. */
                //module.param (moduleParam);
                module.applyDefaultParams();
                module.show  (handler);
            }
            else if (aboutHandler != null) {  /* Show info about handler. */
                handler.show  (aboutHandler);
            }
            else {                            /* Show info about application */
                app.show (handler);
            }
    	}
    	else {
        	// initiate the command parameters for the zip module and gzip module
            if (module instanceof PTC_ZipModule_1_1)
            	module.param(zipModelCommand);
            else if (module instanceof PTC_GzipModule_1_1)
            	module.param(gzipModelCommand);

            for (int i=0; i<dirFileOrUri.length; i++) {
                if (!process (app, module, /*moduleParam, */ handler, /*handlerParam,*/
    			   dirFileOrUri[i])) {
                        break;
                }
    	    }
    	}
    
    	handler.showFooter ();                /* Show handler footer info. */
    }
    
    public static void removeNS( Element elem )    {
        elem.remove( elem.getNamespace() ); 
        elem.setQName( new QName(elem.getQName().getName(), Namespace.NO_NAMESPACE) );
        // fix children
        List children = elem.elements();
        for ( int i = 0; i < children.size(); i++ )
        {
            Element child = (Element)children.get(i);
            removeNS( child);
        }
    }    
    
    /**
     * Given INP
     * @param kobj
     * @throws DocumentException 
     * @throws ParseException 
     */
    public void parseXml(JhoveInfo kobj, StringWriter swriter) throws DocumentException, ParseException {
    	StringBuffer xmldata = new StringBuffer(swriter.toString());
    	kobj.setMetaxml(xmldata);
		Document jdoc = DocumentHelper.parseText(xmldata.toString());
	    Element root = jdoc.getRootElement();
	    removeNS(root);
	    String statusstr = jdoc.valueOf("/jhove/repInfo/status"); 
	    //kobj.setStatus(statusstr);
	    if (/*statusstr.indexOf("not valid") != -1 || */statusstr.indexOf("Not well-formed") != -1) {
	    	kobj.setValid(false);
	    }
	    else {
	    	kobj.setValid(true);
	    }
	    kobj.setCheckSum_CRC32(jdoc.valueOf("/jhove/repInfo/checksums/checksum[@type='CRC32']"));
	    kobj.setChecksum_MD5(jdoc.valueOf("/jhove/repInfo/checksums/checksum[@type='MD5']"));
	    kobj.setChecksum_SHA(jdoc.valueOf("/jhove/repInfo/checksums/checksum[@type='SHA-1']"));
	    kobj.setMIMEtype(jdoc.valueOf("/jhove/repInfo/mimeType"));
	    kobj.setSize(Long.parseLong(jdoc.valueOf("/jhove/repInfo/size")));
	    //try {
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	    	kobj.setDateModified(sdf.parse(jdoc.valueOf("/jhove/repInfo/lastModified")));
	   
	    String format = jdoc.valueOf("/jhove/repInfo/format"); 
	    kobj.setFormat(format);
	    if (format.equalsIgnoreCase("MP3")) {
	    	String layer = jdoc.valueOf("/jhove/repInfo/properties/property/values/property[name='LayerDescription']/values/value");
	    	String version = jdoc.valueOf("/jhove/repInfo/properties/property/values/property[name='MPEG Audio Version ID']/values/value");
	    	kobj.setVersion(version + ", Layer " + layer);
	    }
	    else {
	    	kobj.setVersion(jdoc.valueOf("/jhove/repInfo/version"));
	    }
	    kobj.setReportingModule(jdoc.valueOf("/jhove/repInfo/reportingModule"));
	    String status = kobj.getStatus();
	    if((status == null || status.length() == 0) || !"BYTESTREAM".equalsIgnoreCase(format))
	    	kobj.setStatus(statusstr);
	    
		// image resolution
	    String imageWidth = jdoc.valueOf("//imageWidth");
	    String imageLength = jdoc.valueOf("//imageHeight");
	    if(imageWidth != null && imageLength != null && imageWidth.length() > 0)
		{
	    	kobj.setQuality(imageWidth + "x" + imageLength);
		}

		// WAV bit/sample/channels
		String abits1 = jdoc.valueOf( "//bitDepth" );
		String afreq1 = jdoc.valueOf( "//sampleRate" );
		String achan1 = jdoc.valueOf( "//numChannels" );
		if ( nblank(abits1) || nblank(afreq1) || nblank(achan1) )
		{
			String qual = audioQuality(abits1, afreq1, "Hz", achan1 );
			kobj.setQuality( qual );
		}

		// MP3 bit/sample/channels
		String abits2 = valueOf( jdoc, "Bitrate Index" );
		String afreq2 = valueOf( jdoc, "Sampling rate frequency Index" );
      		String achan2 = valueOf( jdoc, "Channel Mode" );
		if (nblank(abits2) || nblank(afreq2) || nblank(achan2))
		{
			String qual = audioQuality(abits2, afreq2, "kHz", achan2 );
			kobj.setQuality( qual );
		}

		List resultnodes = jdoc.selectNodes("/jhove/repInfo/messages/message[@severity='error']");
		for (int r = 0; resultnodes != null && r < resultnodes.size(); r++)
		{
			Object noderesult = resultnodes.get(r);
			if (noderesult instanceof Node) {
				Node nt = (Node)noderesult;
				kobj.setMessage(nt.getStringValue());
			}
		}				
    }
	private static String valueOf( Document doc, String property )
	{
		return doc.valueOf("//property[name='" + property + "']/values/value");
	}
	private static boolean nblank( String s )
	{
		return s != null && !s.trim().equals("");
	}
	private static String audioQuality( String bits, String freq, String units,
		String chan )
	{
		String qual = "";
		if ( bits != null ) { qual += bits + "-bit"; }
		if ( freq != null )
		{
			if ( !qual.equals("") ) { qual += ", "; }
			qual += freq + " " + units;
		}
		if ( chan != null )
		{
			if ( !qual.equals("") ) { qual += ", "; }
			if ( chan.equals("1") )
			{
				qual += "Single channel (Mono)";
			}
			else if ( chan.equals("2") )
			{
				qual += "Dual channel (Stereo)";
			}
			else if ( chan.indexOf("channel") == -1 )
			{
				qual += chan + " channel";
			}
			else
			{
				qual += chan;
			}
		}

		return qual;
	}
	
	private static synchronized void initModuleMap() throws Exception {
		if(_moduleMap == null){
		if (_moduleNames.length != _fileExten.length) {
			throw new Exception("ModuleNames and fileExten does not match");
			//return false;
		}
		_moduleMap = new HashMap(_moduleNames.length);
		for (int i=0; i<_moduleNames.length; i++) {
			_moduleMap.put(_fileExten[i], _moduleNames[i]);
		}
		//return true;
		}
	}
	
	/**
	 * Note: This code could be initialized only once in the constructor
	 * but based on the comments from Jhove code it is not advisable to do so
	 * 
	 * The JHOVE engine, providing all base services necessary to build an
     * application.   More than one JhoveBase may be instantiated and process files in
     * concurrent threads.  Any one instance must not be multithreaded.
 	 * @return
	 * @throws Exception 
	 * @throws Exception 
	 */
	/*private synchronized void initJhoveEngine() throws Exception {
		if(jhoveconf == null)
			throw new Exception("Configuration file for the Jhove Engine has not set yet.");
		if(_jebase == null){
			String saxClass = MyJhoveBase.getSaxClassFromProperties();
			try {
				_jebase = new MyJhoveBase();
				_jebase.init(MyJhoveBase.jhoveconf, saxClass);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				_jebase = null;
				throw e;
			}
		}
	}*/
	
	public JhoveInfo getJhoveMetaData(String srcFileName) throws Exception {
		File file = new File(srcFileName);
		JhoveInfo dataObj = new JhoveInfo();
		dataObj.setLocalFileName(file.getName());
		dataObj.setFilePath(file.getParent());
		/*if (_jebase == null) {
			initJhoveEngine(); 			
		}*/
		resetAbort ();
		if (!file.canRead()) {
			String emsg = "Can't read file for Jhove analysis: " + dataObj.getFilePath() + File.separatorChar + dataObj.getLocalFileName();
			throw new Exception (emsg);										
		}
		JhoveAnalysisProgress jprogress = new JhoveAnalysisProgress();
		jprogress.set_filesize(file.length());
		setCallback(jprogress);		
		String[] paths = new String[1];
		paths[0] = file.getAbsolutePath();
		setShowRawFlag (true);
		setChecksumFlag (true);
//		_jebase.setLogLevel("ALL");
		
		StringWriter swriter = new StringWriter();
		PrintWriter kwriter = new PrintWriter(swriter); 	  
		 App _jeapp = new App (MyJhoveBase.jhvname, MyJhoveBase.jvhrel, 
				 MyJhoveBase.jhvdate, null, MyJhoveBase.jvhrights);   //Moved here to make it thread safe
		 
		 //Module selection
		Module defaultModule = null;
		int indx = srcFileName.lastIndexOf(".");
		if (indx >= 0) {
			String ext = srcFileName.substring(indx);
			String selectedModule = null;
			if (_moduleMap.containsKey(ext))
				selectedModule = (String) _moduleMap.get(ext);
			else if (ext.indexOf("out") >= 0 || Arrays.asList(BYTESTREAM_MODULE_EXTS).contains(ext.toLowerCase()))
				selectedModule = BYTESTREAM;

			if (StringUtils.isNotBlank(selectedModule))
				defaultModule = (Module) getModuleMap().get (selectedModule.toLowerCase ());
		}
		
		try {
			dispatch (_jeapp,
					defaultModule,
	                null,   // AboutHandler
	                new XmlHandler(),
	                kwriter,   // output
	                paths);
			parseXml(dataObj, swriter);
			if (!dataObj.getValid()) {
				swriter.close();
				kwriter.close();
				swriter = new StringWriter();
				kwriter = new PrintWriter(swriter);
				Module bytestreamModule = (Module) getModuleMap().get("bytestream");

				// keep the original formatName and mimeType
				String mimeType = dataObj.getMIMEtype();
				String formatName = dataObj.getFormat();
				dispatch (_jeapp,
						bytestreamModule,
		                null,   // AboutHandler
		                new XmlHandler(),
		                kwriter,   // output
		                paths);
				parseXml(dataObj, swriter);
				if (defaultModule != null) {
					if (mimeType != null && mimeType.length() > 0)
						dataObj.setMIMEtype(mimeType);
					if (formatName != null && formatName.length() > 0)
						dataObj.setFormat(formatName);
				}
			}
		}
		catch (Exception e)
		{
			log.warn("Jhove analysis error", e);
			if(srcFileName.endsWith(".pdf") || srcFileName.endsWith(".PDF"))
			{
				//Accept PDF file.
				swriter.close();
				kwriter.close();
				swriter = new StringWriter();
				kwriter = new PrintWriter(swriter);
				Module bytestreamModule = (Module) getModuleMap().get("bytestream");

				dispatch (_jeapp,
						bytestreamModule,
		                null,   // AboutHandler
		                new XmlHandler(),
		                kwriter,   // output
		                paths);
				parseXml(dataObj, swriter);
			}else
				throw new Exception(e);			
		}finally{
			swriter.close();
			kwriter.close();
		}
		if (!dataObj.getValid())
			throw new Exception("Unable to extract file: " + srcFileName);

		if(indx > 0){
			String fileExt = srcFileName.substring(indx);
			if(MEDIA_FILES.indexOf(fileExt.toLowerCase()) >= 0)
			{
				Map<String,String> ffmpegInfo = FfmpegUtil.executeInquiry(
					srcFileName, ffmpegCommand
				);
				dataObj.setDuration( ffmpegInfo.get("duration") );

				String audioFormat = ffmpegInfo.get("audio");
				String videoFormat = ffmpegInfo.get("video");
				if ( dataObj.getQuality() == null
					|| dataObj.getQuality().equals("") )
				{
					if ( audioFormat != null && videoFormat != null )
					{
						dataObj.setQuality(
							"video: " + videoFormat + "; audio: " + audioFormat
						);
					}
					else if ( audioFormat != null )
					{
						dataObj.setQuality( audioFormat );
					}
					else if ( videoFormat != null
						&& videoFormat.startsWith("png, ") )
					{
						dataObj.setQuality( videoFormat.substring(5) );
					}
					else if ( videoFormat != null )
					{
						dataObj.setQuality( videoFormat );
					}
				}
			}
		}
		return dataObj;
	}

	public static synchronized void setJhoveConfig(String jhoveconf){
		MyJhoveBase.jhoveconf = jhoveconf;
		//_jebase = null;
	}
	
	public static synchronized void setFfmpegCommand(String ffmpegCommand){
		MyJhoveBase.ffmpegCommand = ffmpegCommand;
	}

	/**
	 * Set the command parameter for the zip model
	 */
	public static synchronized void setZipModelCommand(String zipModelCommand){
		MyJhoveBase.zipModelCommand = zipModelCommand;
	}

	/**
	 * Set the command parameter for the gzip model
	 */
	public static synchronized void setGzipModelCommand(String gzipModelCommand){
		MyJhoveBase.gzipModelCommand = gzipModelCommand;
	}
}
