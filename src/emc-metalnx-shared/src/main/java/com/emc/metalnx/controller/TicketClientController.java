/*
 * Copyright (c) 2015-2017, Dell EMC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.emc.metalnx.controller;

import com.emc.metalnx.core.domain.exceptions.DataGridConnectionRefusedException;
import com.emc.metalnx.services.interfaces.TicketClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Controller that will handle anonymous access to collections and data objects using tickets.
 */
@Controller
@Scope(WebApplicationContext.SCOPE_SESSION)
@RequestMapping(value = "/ticketclient")
public class TicketClientController {
    private static final Logger logger = LoggerFactory.getLogger(TicketClientController.class);

    @Autowired
    private TicketClientService ticketClientService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(@RequestParam("ticketstring") String ticketString, @RequestParam("ticketpath") String path)
            throws DataGridConnectionRefusedException {
        logger.info("Accessing ticket {} on {}", ticketString, path);
        return "tickets/ticketclient";
    }

    @RequestMapping(value = "/{ticketstring}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void upload(@PathVariable("ticketstring") String ticketString, HttpServletRequest request)
            throws DataGridConnectionRefusedException, IOException {
        logger.info("Uploading files using ticket: {}", ticketString);

        if (!(request instanceof MultipartHttpServletRequest)) {
            logger.error("Request is not a multipart request. Stop.");
            return;
        }

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartRequest.getFile("file");
        String destPath = multipartRequest.getParameter("destPath");

        File file = multipartToFile(multipartFile);
        ticketClientService.transferFileToIRODSUsingTicket(ticketString, file, destPath);
    }

    @RequestMapping(value = "/{ticketstring}", method = RequestMethod.GET)
    public void download(@PathVariable("ticketstring") String ticketString, @RequestParam("path") String path,
                       HttpServletResponse response) throws DataGridConnectionRefusedException, IOException {
        logger.info("Getting files using ticket: {}", ticketString);
        InputStream inputStream = ticketClientService.getFileFromIRODSUsingTicket(ticketString, path);
        FileCopyUtils.copy(inputStream, response.getOutputStream()); // takes care of closing streams
        ticketClientService.deleteTempTicketDir();
    }

    /**
     * Converts a multipart file comming from an HTTP request into a File instance.
     * @param multipartFile file uploaded
     * @return File instance
     * @throws IllegalStateException
     * @throws IOException
     */
    private File multipartToFile(MultipartFile multipartFile) throws IllegalStateException, IOException {
        File convFile = new File(multipartFile.getName());
        multipartFile.transferTo(convFile);
        return convFile;
    }
}
