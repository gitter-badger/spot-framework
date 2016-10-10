package at.spot.core.remote.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import at.spot.core.data.model.Item;
import at.spot.core.infrastructure.service.ConfigurationService;
import at.spot.core.infrastructure.service.TypeService;
import at.spot.core.remote.annotation.Get;
import at.spot.core.remote.data.GenericItemData;
import at.spot.core.remote.transformer.JsonResponseTransformer;
import spark.Request;
import spark.Response;

@Service
public class TypeSystemRestService extends AbstractHttpService {

	private static final String CONFIG_PORT_KEY = "spot.service.remotetypeservice.port";
	
	@Autowired
	protected TypeService typeService;
	
	@Autowired
	protected ConfigurationService configurationService;
	
	@Autowired
	protected Converter<Class<? extends Item>, GenericItemData> itemTypeConverter;

	@Get(pathMapping = "/types/", mimeType="application/json", responseTransformer=JsonResponseTransformer.class)
	public Object getTypes(Request request, Response response) {
		List<GenericItemData> types = new ArrayList<>();

		for (Class<? extends Item> t : typeService.getAvailableTypes()) {
			GenericItemData d = itemTypeConverter.convert(t);
			types.add(d);
		}
		
		return types;
	}

	@Override
	protected void setPort(int port) {
		super.setPort(configurationService.getInteger(CONFIG_PORT_KEY));
	}
}
