package org.loopingdoge.acme.services;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.loopingdoge.acme.jolie.cadastre.Cadastre;
import org.loopingdoge.acme.jolie.cadastre.CadastreService;
import org.loopingdoge.acme.jolie.cadastre.Coordinate;
import org.loopingdoge.acme.model.House;
import org.loopingdoge.acme.utils.HouseDatabase;

import javax.xml.ws.Holder;
import java.util.logging.Logger;

/**
 * in:  House house
 * out: String cadastrialError
 *      Coordinate cadastrialCoordinates?
 */
public class SendAddressToCadastre implements JavaDelegate {

    private final static Logger logger = Logger.getLogger("SendAddressToCadastre");

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("service started");

        execution.setVariable("chosenHouse", HouseDatabase.getHouse(0));    // For debug purposes

        House house = (House) execution.getVariable("chosenHouse");
        String address = house.getAddress().toCadastreFormat();

        logger.info(address);

        final Bus defaultBus = BusFactory.getDefaultBus();
        final ConduitInitiatorManager extension = defaultBus.getExtension(ConduitInitiatorManager.class);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http/", new HTTPTransportFactory());

        CadastreService service = new CadastreService();
        Cadastre server = service.getCadastreServicePort();

        Holder<Coordinate> coordinatesResult = new Holder<>();
        Holder<String> errorResult = new Holder<>();

        server.cadastrialCoordinates(address, coordinatesResult, errorResult);

        logger.info("Coordinates: " + coordinatesResult.value.getNord() + " " + coordinatesResult.value.getEast());
        logger.info("Error: " +  errorResult.value);

        String cadastrialError = errorResult.value;

        execution.setVariableLocal("cadastrialError", cadastrialError);

        if (!cadastrialError.equals("")) {
            execution.setVariable("cadastrialCoordinates", coordinatesResult.value);
        }
    }

}
