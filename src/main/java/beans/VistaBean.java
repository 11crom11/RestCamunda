package beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.annotation.FacesConfig;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import control.GestorCamunda;

@ViewScoped
@FacesConfig
@Named
public class VistaBean implements Serializable {

	private static final long serialVersionUID = 5367460900159271384L;
	
	@Inject
	private SesionBean sesionBean;
	
	private String testVistaBean = null;	
	private int numCambiante = -1;
	private List<String> procesosDefinicion;
	private HashMap<String, String> procesosDefinicionKey;
	private String procesoSeleccionado;
	private String mensajeProcesoArrancado;
	private LinkedHashMap<String, List<String>> tareasUsuario;
	private String iconoIniciarSesion;
	
	private GestorCamunda gestorCamunda;
	
	@PostConstruct
	public void inicio() {
		this.testVistaBean = "testVistaBean";
		this.numCambiante = 0;
		
		this.gestorCamunda = new GestorCamunda("http://localhost:8080");
		this.procesosDefinicionKey = new HashMap<String, String>();
		this.tareasUsuario = new LinkedHashMap<String, List<String>>();
		this.procesoSeleccionado = "";
		this.mensajeProcesoArrancado = "";
		
		if(this.sesionBean.getMatriculaUsuario().equals("No conectado")) {
			this.iconoIniciarSesion = "person-x-fill.svg";
		}
		else {
			this.iconoIniciarSesion = "person-check-fill.svg";
		}
	}
	
	public void rellenaTexto() {
		this.numCambiante = this.numCambiante + 1;
		actualizarID("idFormTextoCambiante:idLabelCambiante");
	}
	
	public void mostrarDefinicionProcesosCamunda() {
		
		HashMap<String, String> listadoProcesos;
		String codeRespose;
		
		listadoProcesos = this.gestorCamunda.obtenerListadoDefinicionProcesosCamundaREST();
		
		if(listadoProcesos.containsKey("OKCode")) {
			codeRespose = listadoProcesos.get("OKCode");
			listadoProcesos.remove("OKCode");
			
			this.procesosDefinicionKey = listadoProcesos;
			
			actualizarID("idFormListadoProcesos:idListProcDef");
			actualizarID("idFormGestionProcesos:idDesplegableProcDef");
		}
		else if (listadoProcesos.containsKey("errorCode")){
			codeRespose = listadoProcesos.get("errorCode");
			listadoProcesos.remove("errorCode");
			
			if(codeRespose.equals("400")) {
				//TODO
			}
		}
		else if(listadoProcesos.containsKey("error")) {
			codeRespose = listadoProcesos.get("error");
			
			//TODO
		}
	}
	
	public void arrancarProcesoCamunda() {
		Map<String, String> arrancado;
		String keyProcess;
		String codeResponse;
		 	 
		keyProcess = this.procesosDefinicionKey.get(this.procesoSeleccionado);
		 
		if(keyProcess != null) {
			arrancado = this.gestorCamunda.arrancarProcesoCamundaByKeyREST(keyProcess);
			
			if(arrancado.containsKey("OKCode")) {
				codeResponse = arrancado.get("OKCode");
				
				if(codeResponse.equals("200")) {
					this.mensajeProcesoArrancado = "El proceso " + this.procesoSeleccionado + " ha sido arrancado.";
					actualizarID("idFormGestionProcesos:idProcesoArrancado");
				}
				
			}
			else if(arrancado.containsKey("errorCode")) {
				codeResponse = arrancado.get("errorCode");
				
				if(codeResponse.equals("400")) {
					//TODO
				}
				else if(codeResponse.equals("404")) {
					this.mensajeProcesoArrancado = "El proceso " + this.procesoSeleccionado + " no ha podido ser arrancado. No existe.";
					actualizarID("idFormGestionProcesos:idProcesoArrancado");
				}
				else if(codeResponse.equals("500")) {
					this.mensajeProcesoArrancado = "El proceso " + this.procesoSeleccionado + " no ha podido ser arrancado. Problemas con el servidor.";
					actualizarID("idFormGestionProcesos:idProcesoArrancado");
				}
			}
			else if (arrancado.containsKey("error")) {
				this.mensajeProcesoArrancado = "El proceso " + this.procesoSeleccionado + " no ha podido ser arrancado. Excepción en la clase GestorCamunda.";
				actualizarID("idFormGestionProcesos:idProcesoArrancado");
			}
		}		
	}
	
	public void finalizarTareaPendiente(String rowNumber) {
		
		String idTarea;
		int status;
		FacesMessage message = null;
		PrimeFaces current;
		
		idTarea = (String) this.tareasUsuario.keySet().toArray()[Integer.parseInt(rowNumber)];
		
		status = this.gestorCamunda.finalizarTareaPendiente(idTarea);
		
		if(status == 204) {
			message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Gestor de tareas pendientes", "La tarea ha sido completada correctamente.");
			this.tareasUsuario.remove(idTarea);
		}
		else if(status == 400) {
			message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Gestor de tareas pendientes (error 400)", "El servidor devolvió ERROR 400.");
		}
		else if(status == 500) {
			message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Gestor de tareas pendientes (error 500)", "Error en el gestor de procesos. No se ha podido finalizar la tarea.");
		}
		else {
			message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Gestor de tareas pendientes (-1)", "Error desconocido en programación.");
		}
		
		current = PrimeFaces.current();
		current.dialog().showMessageDynamic(message);
		
		this.actualizarID("idFormMatriculaSesion:idListTareas");
	}
	
	public void mostrarTareasPendientesUsuario() {
		
		if(this.sesionBean.getMatriculaUsuario() != null && !this.sesionBean.getMatriculaUsuario().equals("No conectado")) {
			this.tareasUsuario = this.gestorCamunda.obtenerTareasPendientesUsuarioREST(this.sesionBean.getMatriculaUsuario());
			
			this.actualizarID("idFormMatriculaSesion:idListTareas");
		}
	}
	
	public List<String> recuperarNombresProcesosFromHashMap(){
		
		if(this.procesosDefinicionKey.isEmpty() || this.procesosDefinicionKey == null) {
			return new ArrayList<String>();
		}
		else {
			return Arrays.asList(this.procesosDefinicionKey.keySet().toArray(new String[this.procesosDefinicionKey.size()]));
		}
	}
	
	public List<List<String>> recuperarNombresTareasFromHashMap(){
		if(this.tareasUsuario.isEmpty() || this.tareasUsuario == null) {
			return new ArrayList<List<String>>();
		}
		else {
			return this.tareasUsuario.values().stream().toList();
		}
	}
	
	public void ajustarUsuarioInicioSesion() {
			
		this.actualizarID("idFormMatriculaSesion:idMatriculaOut");
		
		this.iconoIniciarSesion = "person-check-fill.svg";
		this.actualizarID("idFormMatriculaSesion:iconoSesion");
	}
	
	public void ajustarUsuarioCierreSesion() {
		this.sesionBean.cerrarSesion();
	}
	
	public void actualizarID(String idElemento) {
		if(idElemento != null) {
			try {
				PrimeFaces.current().ajax().update(idElemento);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public SesionBean getSesionBean() {
		return sesionBean;
	}

	public void setSesionBean(SesionBean sesionBean) {
		this.sesionBean = sesionBean;
	}

	public String getTestVistaBean() {
		return testVistaBean;
	}

	public void setTestVistaBean(String testVistaBean) {
		this.testVistaBean = testVistaBean;
	}

	public int getNumCambiante() {
		return numCambiante;
	}

	public void setNumCambiante(int numCambiante) {
		this.numCambiante = numCambiante;
	}
	
	public List<String> getProcesosDefinicion() {
		return procesosDefinicion;
	}

	public void setProcesosDefinicion(List<String> procesosDefinicion) {
		this.procesosDefinicion = procesosDefinicion;
	}
	
	public HashMap<String, String> getProcesosDefinicionKey() {
		return procesosDefinicionKey;
	}

	public void setProcesosDefinicionKey(HashMap<String, String> procesosDefinicionKey) {
		this.procesosDefinicionKey = procesosDefinicionKey;
	}

	public String getProcesoSeleccionado() {
		return procesoSeleccionado;
	}

	public void setProcesoSeleccionado(String procesoSeleccionado) {
		this.procesoSeleccionado = procesoSeleccionado;
	}

	public String getMensajeProcesoArrancado() {
		return mensajeProcesoArrancado;
	}

	public void setMensajeProcesoArrancado(String mensajeProcesoArrancado) {
		this.mensajeProcesoArrancado = mensajeProcesoArrancado;
	}

	public String getIconoIniciarSesion() {
		return iconoIniciarSesion;
	}

	public void setIconoIniciarSesion(String iconoIniciarSesion) {
		this.iconoIniciarSesion = iconoIniciarSesion;
	}

}
