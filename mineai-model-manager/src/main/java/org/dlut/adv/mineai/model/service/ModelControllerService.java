package org.dlut.adv.mineai.model.service;

import org.dlut.adv.mineai.core.entity.Controller;
import org.dlut.adv.mineai.model.repository.ModelControllerRepo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ModelControllerService {

    @Resource
    ModelControllerRepo modelControllerRepo;

    public List<Controller> findControllersByStatusAndStatusUsingAndArchitecture(){
        return modelControllerRepo.findControllersByStatusAndStatusUsingAndArchitecture(Controller.STATUS_ON, Controller.STATUS_WORKING, Controller.AMD64);
    }

    public Controller findControllerById(long controllerId) {
        return modelControllerRepo.findControllerById(controllerId);
    }
}
