package gov.hhs.cms.bluebutton.data.model.rif;

import java.util.Optional;
import java.util.function.Function;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

/**
 * Associates a raw RIF value with its (hopefully) decoded {@link Enum}
 * constant.
 * 
 * @param <V>
 *            the raw value type coming in from RIF
 * @param <C>
 *            the {@link Enum} type that the values can be decoded into
 */
public final class CodedValue<V, C> {
	private final V rawValue;
	private final Optional<C> decodedValue;

	/**
	 * Constructs a new {@link CodedValue} instance
	 * 
	 * @param rawValue
	 *            the value to use for {@link #getRawValue()}
	 * @param decoder
	 *            {@link Function} that can be used to decode
	 *            {@link #getRawValue()} into {@link #getDecodedValue()}
	 */
	public CodedValue(V rawValue, Function<V, Optional<C>> decoder) {
		if (decoder == null)
			throw new BadCodeMonkeyException();

		this.rawValue = rawValue;
		this.decodedValue = decoder.apply(rawValue);
	}

	/**
	 * @return the raw value coming in from RIF, which may be <code>null</code>
	 *         or an empty value or really anything
	 */
	public V getRawValue() {
		return rawValue;
	}

	/**
	 * @return the {@link Enum} value that {@link #getRawValue()} was decoded
	 *         into, or {@link Optional#empty()} if it could not be decoded
	 */
	public Optional<C> getDecodedValue() {
		return decodedValue;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((decodedValue == null) ? 0 : decodedValue.hashCode());
		result = prime * result + ((rawValue == null) ? 0 : rawValue.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CodedValue other = (CodedValue) obj;
		if (decodedValue == null) {
			if (other.decodedValue != null)
				return false;
		} else if (!decodedValue.equals(other.decodedValue))
			return false;
		if (rawValue == null) {
			if (other.rawValue != null)
				return false;
		} else if (!rawValue.equals(other.rawValue))
			return false;
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CodedValue [rawValue=");
		builder.append(rawValue);
		builder.append(", decodedValue=");
		builder.append(decodedValue);
		builder.append("]");
		return builder.toString();
	}
}
