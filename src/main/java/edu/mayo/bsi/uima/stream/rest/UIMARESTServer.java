package edu.mayo.bsi.uima.stream.rest;

import edu.mayo.bsi.uima.stream.core.UIMAServerBase;
import edu.mayo.bsi.uima.stream.rest.models.ServerRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class UIMARESTServer extends UIMAServerBase {
    @Override
    public void start() {

    }

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<?> submitJob(@RequestBody ServerRequest req) {
        return null;
    }
}
