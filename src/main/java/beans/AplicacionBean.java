package beans;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Singleton
@Startup
@ApplicationScoped
@Named
public class AplicacionBean implements Serializable {

	private static final long serialVersionUID = 4646138654570660148L;
	
	private String testAplicacion = null;
	
	@PostConstruct
	public void inicio() {
		this.testAplicacion = "testAplicacion";
	}
	
	public String getTestAplicacion() {
		return testAplicacion;
	}

	public void setTestAplicacion(String testAplicacion) {
		this.testAplicacion = testAplicacion;
	}

}
