package validators;

import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import beans.VistaBean;

@FacesValidator(value = "validadorSesion")
public class ValidadorSesion implements Validator {

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		// TODO Auto-generated method stub
		
		String regexp = "t\\d{6}";
		String cadenaInput = value.toString();
		
		if(!Pattern.matches(regexp, cadenaInput)) {
			FacesMessage msg = new FacesMessage("No se ha introducido una matrícula correcta.");
			
			Object vistaContainerBean = component.getValueExpression("containerBean").getValue(context.getELContext());
			
			if(vistaContainerBean instanceof VistaBean) {
				((VistaBean) vistaContainerBean).setIconoIniciarSesion("exclamation-triangle-fill.svg");
				((VistaBean) vistaContainerBean).actualizarID("idFormMatriculaSesion:iconoSesion");
			}
			
			throw new ValidatorException(msg);
		}
	}
}
