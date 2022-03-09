package control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.RuntimeService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import connectjar.org.apache.http.HttpConnection;

public class GestorCamunda {

	private String URLcamunda;

	private ProcessEngine processEngine;
	private RuntimeService runtimeService;

	public GestorCamunda(String url) {
		this.URLcamunda = url;
		this.processEngine = ProcessEngines.getDefaultProcessEngine();
		this.runtimeService = this.processEngine.getRuntimeService();
	}

	public List<String> obtenerListadoProcesosCamunda() {

		List<String> listadoProcesos = new ArrayList<String>();

		return listadoProcesos;
	}
	
	public HashMap<String, String> obtenerListadoDefinicionProcesosCamundaREST() {

		JSONArray jsonResultado;
		HashMap<String, String> listadoProcesos = new HashMap<>();
		
		// URL = "http://localhost:8080/engine-rest/process-definition"		
		jsonResultado = peticionGet(this.URLcamunda + "/engine-rest/process-definition");
		
		if(((JSONObject)jsonResultado.get(0)).has("error")) {
			listadoProcesos.put("error", ((JSONObject)jsonResultado.get(0)).getString("error"));
		}
		else if (((JSONObject)jsonResultado.get(0)).has("errorCode")) {
			String code = ((JSONObject)jsonResultado.get(0)).getString("errorCode");
			
			listadoProcesos.put("errorCode", code);
		}
		else if (((JSONObject)jsonResultado.get(0)).has("OKCode")){
			String code = ((JSONObject)jsonResultado.get(0)).getString("OKCode");
			
			listadoProcesos.put("OKCode", code);
			
			if(code.equals("200")) {
				for(int i = 1; i < jsonResultado.length(); i++) { //El primero elemento el código de respuesta de la petición
									
					JSONObject procesoJSON = jsonResultado.getJSONObject(i);
					
					if(procesoJSON.has("name") && procesoJSON.has("key")) {
						listadoProcesos.put(procesoJSON.getString("name"), procesoJSON.getString("key")); //Al usar el nombre como clave, las definiciones con varias versiones solo apareceran una vez.			
					}
				}
			}
		}
					
		return listadoProcesos;
	}
	
	public Map<String, String> arrancarProcesoCamundaByKeyREST(String key) {
		Map<String, String> code;
		String url;
		JSONArray jsonResultado;
		
		code = new HashMap<String, String>();
						
		url = this.URLcamunda + "/engine-rest/process-definition/key/" + key + "/start";
		
		jsonResultado = peticionPost(url, null);

		if(((JSONObject)jsonResultado.get(0)).has("error")) {
			String codeResponse = ((JSONObject)jsonResultado.get(0)).getString("error");			
			code.put("error", codeResponse);
		}
		else if (((JSONObject)jsonResultado.get(0)).has("errorCode")) {
			String codeResponse = ((JSONObject)jsonResultado.get(0)).getString("errorCode");
			code.put("errorCode", codeResponse);
		}
		else if (((JSONObject)jsonResultado.get(0)).has("OKCode")){
			String codeResponse = ((JSONObject)jsonResultado.get(0)).getString("OKCode");
			code.put("OKCode", codeResponse);
		}
		
		return code;
	}
	
	public LinkedHashMap<String, List<String>> obtenerTareasPendientesUsuarioREST(String usuario){
		String url;
		LinkedHashMap<String, List<String>> tareasUsuario = new LinkedHashMap<String, List<String>>();
		JSONArray jsonResultado;
		JSONObject parameters;	
		Collection<JSONObject> parametroOrden = new ArrayList<JSONObject>();
		
		url = this.URLcamunda + "/engine-rest/task";
		
		parameters = new JSONObject();	
		parameters.put("assignee", usuario);

		parametroOrden.add(new JSONObject().put("sortBy", "created"));
		parametroOrden.add(new JSONObject().put("sortOrder", "asc"));
		
		parameters.put("sorting", parametroOrden);
		
		jsonResultado = this.peticionPost(url, parameters);
		
		
		if(((JSONObject)jsonResultado.get(0)).has("errorCode")) {
			String codeResponse = ((JSONObject)jsonResultado.get(0)).getString("errorCode");
			
			List<String> aux = new ArrayList<String>();
			aux.add(codeResponse);
			tareasUsuario.put("errorCode", aux);
		}
		else if (((JSONObject)jsonResultado.get(0)).has("error")) {
			String codeResponse = ((JSONObject)jsonResultado.get(0)).getString("error");
			
			List<String> aux = new ArrayList<String>();
			aux.add(codeResponse);
			tareasUsuario.put("error", aux);
		}
		else if(((JSONObject)jsonResultado.get(0)).has("OKCode")) {
			for(int i = 0; i < jsonResultado.length(); i++) {
				JSONObject tareaJSON = jsonResultado.getJSONObject(i);
				
				if(tareaJSON.has("id") && tareaJSON.has("name") && tareaJSON.has("created")) {						
					List<String> atributosTarea = new ArrayList<String>();

					//FORMATOS DATE
					//http://tutorials.jenkov.com/java-date-time/parsing-formatting-dates.html
					//BUG -> EL OFFSET POR ZONA +0000 -> +00:00
					//https://stackoverflow.com/questions/43360852/cannot-parse-string-in-iso-8601-format-lacking-colon-in-offset-to-java-8-date
					OffsetDateTime fechaTareaFormatoCamunda = OffsetDateTime.parse(tareaJSON.getString("created"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
					
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");
											
					String fechaTareaFormatoWeb = formatter.format(fechaTareaFormatoCamunda);						

					atributosTarea.add(tareaJSON.getString("name"));
					atributosTarea.add(fechaTareaFormatoWeb);
					tareasUsuario.put(tareaJSON.getString("id"), atributosTarea);
				}
			}
		}
		
		return tareasUsuario;
	}
	
	public int finalizarTareaPendiente(String idTarea) {
		String url;
		JSONArray jsonResultado;
		int status = -1;
		
		url = this.URLcamunda + "/engine-rest/task/" + idTarea + "/complete";
		
		jsonResultado = peticionPost(url, null);
		
		if(((JSONObject)jsonResultado.get(0)).has("errorCode")) {
			String codeResponse = ((JSONObject)jsonResultado.get(0)).getString("errorCode");
			
			status = Integer.parseInt(codeResponse);
		}
		else if (((JSONObject)jsonResultado.get(0)).has("error")) {
			status = -1;
		}
		else if(((JSONObject)jsonResultado.get(0)).has("OKCode")) {
			String codeResponse = ((JSONObject)jsonResultado.get(0)).getString("OKCode");
			
			status = Integer.parseInt(codeResponse);
		}
		
		return status;
	}

	public static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			result.append("&");
		}

		String resultString = result.toString();
		return resultString.length() > 0 ? resultString.substring(0, resultString.length() - 1) : resultString;
	}
	
	private JSONArray peticionGet(String urlRequest) {
		// COMO HACER PETICIONES GET (TUTORIAL) ->
		// https://parzibyte.me/blog/2019/02/14/peticion-http-get-java-consumir-html-json/
		// https://www.baeldung.com/java-http-request
		
		URL url;
		HttpURLConnection conexion;
		BufferedReader rd;
		int status;
		String linea;
		StringBuilder resultado;
		
		JSONArray jsonArrayResultado;
		
		try {
			url = new URL(urlRequest);
			
			conexion = (HttpURLConnection) url.openConnection();
			conexion.setRequestMethod("GET");
			conexion.setConnectTimeout(5000);
			conexion.setReadTimeout(5000);
			
			status = conexion.getResponseCode();
			
			if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_ACCEPTED || 
					status == HttpURLConnection.HTTP_CREATED || status == HttpURLConnection.HTTP_NO_CONTENT ||
					status == HttpURLConnection.HTTP_NOT_AUTHORITATIVE || status == HttpURLConnection.HTTP_PARTIAL ||
					status == HttpURLConnection.HTTP_RESET) {
				
				rd = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
				
				resultado = new StringBuilder();
				
				while ((linea = rd.readLine()) != null) {
					resultado.append(linea);
				}
				
				rd.close();
				
				String res = resultado.toString();
				
				if(!res.substring(0, 1).equals("[")) {
					res = "[" + res + "]";
				}
				
				jsonArrayResultado = new JSONArray();
				Map<String, String> ok = new HashMap<String, String>();
				ok.put("OKCode", Integer.toString(status));
				jsonArrayResultado.put(ok);
				jsonArrayResultado.putAll(new JSONArray(res));
			}
			else {
				rd = new BufferedReader(new InputStreamReader(conexion.getErrorStream()));
				
				resultado = new StringBuilder();
									
				while ((linea = rd.readLine()) != null) {
					resultado.append(linea);
				}
				
				rd.close();
				
				jsonArrayResultado = new JSONArray("[{\"errorCode\": " + "\"" + Integer.toString(status) + "\"}]");
				Map<String, String> aux = new HashMap<String, String>();
				aux.put("message", resultado.toString());
				jsonArrayResultado.put(aux);
			}
			
			conexion.disconnect();
			
			
		}catch (Exception e) {
			e.printStackTrace();
			jsonArrayResultado = new JSONArray("[{\"error\": \"Problema al realizar la petición\"}]");
		}		
		
		return jsonArrayResultado;
	}
	
	private JSONArray peticionPost(String urlRequest, JSONObject parameters) {
		// COMO HACER PETICIONES POST (TUTORIAL) ->
		//https://www.baeldung.com/httpurlconnection-post
		
		URL url;
		HttpURLConnection conexion;
		BufferedReader rd;
		int status;
		String linea;
		StringBuilder resultado;
		
		JSONArray jsonResultado = null;
		
		
		try {
			url = new URL(urlRequest);
			
			try {
				conexion = (HttpURLConnection)url.openConnection();
				conexion.setRequestMethod("POST");
				conexion.setConnectTimeout(5000);
				conexion.setReadTimeout(5000);
				conexion.setRequestProperty("Content-Type", "application/json; utf-8");
				conexion.setRequestProperty("Accept", "application/json");
				conexion.setDoOutput(true);
				
				if(parameters != null) {
					OutputStream os = conexion.getOutputStream();
				    byte[] input = parameters.toString().getBytes("utf-8");
				    os.write(input, 0, input.length);
				    os.close();
				}
				
				status = conexion.getResponseCode();
								
				if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_NO_CONTENT)  {
					
					rd = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
					
					resultado = new StringBuilder();
					
					while ((linea = rd.readLine()) != null) {
						resultado.append(linea);
					}
					
					rd.close();
					
					String res = resultado.toString();
					
					
					//Algunas peticiones no devuelven resultado, otras un JSON y otras un array de JSON
					if(res.equals("")) {
						res = "[" + res + "]";
					}
					else if(!res.substring(0, 1).equals("[")) {
						res = "[" + res + "]";
					}
					
					jsonResultado = new JSONArray();
					Map<String, String> ok = new HashMap<String, String>();
					ok.put("OKCode", Integer.toString(status));
					jsonResultado.put(ok);
					jsonResultado.putAll(new JSONArray(res));
				}
				else {
					rd = new BufferedReader(new InputStreamReader(conexion.getErrorStream()));
					
					resultado = new StringBuilder();
										
					while ((linea = rd.readLine()) != null) {
						resultado.append(linea);
					}
					
					rd.close();
					
					jsonResultado = new JSONArray("[{\"errorCode\": " + "\"" + Integer.toString(status) + "\"}]");
					Map<String, String> aux = new HashMap<String, String>();
					aux.put("message", resultado.toString());
					jsonResultado.put(aux);
				}
				
				conexion.disconnect();
				
			} catch (IOException e) {
				
				e.printStackTrace();
				jsonResultado = new JSONArray("[{\"error\": \"Problema al realizar la petición\"}]");
			}
			
			
		} catch (MalformedURLException e) {
			
			e.printStackTrace();
			jsonResultado = new JSONArray("[{\"error\": \"Problemas con la URL de la petición\"}]");
		}

		return jsonResultado;
	}
}
