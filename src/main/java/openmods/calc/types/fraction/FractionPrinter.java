package openmods.calc.types.fraction;

import openmods.calc.IValuePrinter;
import openmods.config.simpler.Configurable;
import org.apache.commons.lang3.math.Fraction;

public class FractionPrinter implements IValuePrinter<Fraction> {

	@Configurable
	public boolean properFractions;

	@Configurable
	public boolean expand;

	@Override
	public String toString(Fraction value) {
		if (expand) return Double.toString(value.doubleValue());
		return properFractions? value.toProperString() : value.toString();
	}

}
