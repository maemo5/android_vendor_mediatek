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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Conference-Info document
 *
 * @author jexa7410
 */
public class GroupListDocument {
    

    /**
     * Conference URI
     */
    private String entity;

    /**
     * State attribute
     */
    private String state;


    /**
     * List of users
     */
    private List<String> groupIds = new ArrayList<String>();

    
    /**
     * Constructor
     * 
     * @param entity conference URI
     * @param state state attribute
     */
    public GroupListDocument(String entity, String state) {
        this.entity = entity;
        this.state = state;
    }

    /**
     * Return the conference URI
     *
     * @return conference URI
     */
    public String getEntity() {
        return entity;
    }

    /**
     * Reeturn the state
     *
     * @return state
     */
    public String getState() {
        return state;
    }

    /**
     * Add a user
     *
     * @param user
     */
    public void addGroupId(String id) {
        groupIds.add(id);
    }

    /**
     * Get the list of users
     *
     * @return list of users
     */
    public List<String> getGroupIds() {
        return groupIds;
    }

    

}
