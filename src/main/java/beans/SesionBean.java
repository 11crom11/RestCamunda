package beans;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

@SessionScoped
@Named
public class SesionBean implements Serializable {

	private static final long serialVersionUID = 2997435303574900097L;
	
	@Inject
	private AplicacionBean aplicacionBean;
	
	private String testSesionBean = null;
	private String matriculaUsuario;
	

	@PostConstruct
	public void inicio() {
		this.testSesionBean = "testSesionBean";
		this.matriculaUsuario = "No conectado";
		
		/*Principal principalTmp = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
		
		if(principalTmp != null) {
			this.matriculaUsuario = principalTmp.getName();
		}
		else {
			this.matriculaUsuario = null;
		}*/
		
		
	}
	
	public void cerrarSesion() {
		ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
		
		this.matriculaUsuario = "No conectado";
		
		try {
			ec.invalidateSession();
			ec.redirect(ec.getRequestContextPath() + "/index.jsf");
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	public AplicacionBean getAplicacionBean() {
		return aplicacionBean;
	}

	public void setAplicacionBean(AplicacionBean aplicacionBean) {
		this.aplicacionBean = aplicacionBean;
	}

	public String getTestSesionBean() {
		return testSesionBean;
	}

	public void setTestSesionBean(String testSesionBean) {
		this.testSesionBean = testSesionBean;
	}

	public String getMatriculaUsuario() {
		return matriculaUsuario;
	}

	public void setMatriculaUsuario(String matriculaUsuario) {
		this.matriculaUsuario = matriculaUsuario;
	}
}
