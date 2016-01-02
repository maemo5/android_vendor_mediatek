/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.im.chat.event;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Conference-Info parser
 *
 * @author jexa7410
 */
public class GroupListParser extends DefaultHandler {
	
	/* Conference-Info SAMPLE:
	
    </conference-info>
   */
	
	private StringBuffer accumulator;
	private GroupListDocument groupList = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param inputSource Input source
     * @throws Exception
     */
    public GroupListParser(InputSource inputSource) throws Exception {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
	}

	public GroupListDocument getGroupList() {
		return groupList;
	}

	public void startDocument() {
		if (logger.isActivated()) {
			logger.debug("Start document");
		}
		accumulator = new StringBuffer();
	}

	public void characters(char buffer[], int start, int length) {
		accumulator.append(buffer, start, length);
	}

	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		accumulator.setLength(0);

		if (localName.equals("conference-info")) {
			String entity = attr.getValue("entity").trim();
			String state = attr.getValue("state").trim();
			groupList = new GroupListDocument(entity, state);
		} else
		if (localName.equals("grouplist-ver")) {
			String version = attr.getValue("version").trim();
		}
	}

	public void endElement(String namespaceURL, String localName, String qname) {
		if (localName.equals("groupid")) {		
		   groupList.addGroupId(accumulator.toString().trim());
		} 
	}

	public void endDocument() {
		if (logger.isActivated()) {
			logger.debug("End document");
		}
	}
}
