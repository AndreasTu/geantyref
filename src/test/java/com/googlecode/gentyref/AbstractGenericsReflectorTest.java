package com.googlecode.gentyref;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.googlecode.gentyref.TypeToken;


import junit.framework.TestCase;

public abstract class AbstractGenericsReflectorTest extends TestCase {
	/**
	 * A constant that's false, to use in an if() block for code that's only there to show that it compiles.
	 * This code "proves" that the test  is an actual valid test case, by showing the compiler agrees.
	 * But some of the code should not actually be executed, because it might throw exceptions
	 * (because we're too lazy to initialize everything). 
	 */
	private static final boolean COMPILE_CHECK = false;
	
	private static final TypeToken<ArrayList<String>> ARRAYLIST_OF_STRING = new TypeToken<ArrayList<String>>(){};
	private static final TypeToken<List<String>> LIST_OF_STRING = new TypeToken<List<String>>(){};
	private static final TypeToken<Collection<String>> COLLECTION_OF_STRING = new TypeToken<Collection<String>>(){};
	
	private static final TypeToken<ArrayList<List<String>>> ARRAYLIST_OF_LIST_OF_STRING = new TypeToken<ArrayList<List<String>>>(){};
	private static final TypeToken<List<List<String>>> LIST_OF_LIST_OF_STRING = new TypeToken<List<List<String>>>(){};
	private static final TypeToken<Collection<List<String>>> COLLECTION_OF_LIST_OF_STRING = new TypeToken<Collection<List<String>>>(){};
	
	private static final TypeToken<ArrayList<? extends String>> ARRAYLIST_OF_EXT_STRING = new TypeToken<ArrayList<? extends String>>(){};
	private static final TypeToken<Collection<? extends String>> COLLECTION_OF_EXT_STRING = new TypeToken<Collection<? extends String>>(){};
	
	private static final TypeToken<Collection<? super String>> COLLECTION_OF_SUPER_STRING = new TypeToken<Collection<? super String>>(){};
	
	private static final TypeToken<ArrayList<List<? extends String>>> ARRAYLIST_OF_LIST_OF_EXT_STRING = new TypeToken<ArrayList<List<? extends String>>>(){};
	private static final TypeToken<List<List<? extends String>>> LIST_OF_LIST_OF_EXT_STRING = new TypeToken<List<List<? extends String>>>(){};
	private static final TypeToken<Collection<List<? extends String>>> COLLECTION_OF_LIST_OF_EXT_STRING = new TypeToken<Collection<List<? extends String>>>(){};
	
	private final ReflectionStrategy strategy;
	
	class Box<T> implements WithF<T>, WithFToken<TypeToken<T>> {
		public T f;
	}
	
	public AbstractGenericsReflectorTest(ReflectionStrategy strategy) {
		this.strategy = strategy;
	}
	
	private boolean isSupertype(TypeToken<?> supertype, TypeToken<?> subtype) {
		return strategy.isSupertype(supertype.getType(), subtype.getType());
	}
	
	/**
	 * Test if superType is seen as a real (not equal) supertype of subType.
	 */
	private void testRealSupertype(TypeToken<?> superType, TypeToken<?> subType) {
		// test if it's really seen as a supertype
		assertTrue(isSupertype(superType, subType));
		
		// check if they're not seen as supertypes the other way around
		assertFalse(isSupertype(subType, superType));
	}
	
	private <T> void checkedTestInexactSupertype(TypeToken<T> expectedSuperclass, TypeToken<? extends T> type) {
		testInexactSupertype(expectedSuperclass, type);
	}
	
	/**
	 * Checks if the supertype is seen as a supertype of subType.
	 * But, if superType is a Class or ParameterizedType, with different type parameters.
	 */
	private void testInexactSupertype(TypeToken<?> superType, TypeToken<?> subType) {
		testRealSupertype(superType, subType);
		strategy.testInexactSupertype(superType.getType(), subType.getType());
	}
	
	/**
	 * Like testExactSuperclass, but the types of the arguments are checked so only valid test cases can be applied
	 */
	private <T> void checkedTestExactSuperclass(TypeToken<T> expectedSuperclass, TypeToken<? extends T> type) {
		testExactSuperclass(expectedSuperclass, type);
	}
	
	/**
	 * Like testExactSuperclass, but the types of the arguments are checked so only valid test cases can be applied
	 */
	private <T> void assertCheckedTypeEquals(TypeToken<T> expected, TypeToken<T> type) {
		assertEquals(expected, type);
	}
	
	/**
	 * Checks if the supertype is seen as a supertype of subType.
	 * SuperType must be a Class or ParameterizedType, with the right type parameters.
	 */
	private void testExactSuperclass(TypeToken<?> expectedSuperclass, TypeToken<?> type) {
		testRealSupertype(expectedSuperclass, type);
		strategy.testExactSuperclass(expectedSuperclass.getType(), type.getType());
	}
	
	protected static Class<?> getClassType(Type type) {
		if (type instanceof Class) {
			return (Class<?>)type;
		} else {
			ParameterizedType pType = (ParameterizedType) type;
			return (Class<?>)pType.getRawType();
		}
	}
	
	private TypeToken<?> getFieldType(TypeToken<?> forType, String fieldName) {
		return getFieldType(forType.getType(), fieldName);
	}
	
	/**
	 * Marker interface to mark the type of the field f.
	 * Note: we could use a method instead of a field, so the method could be in the interface,
	 * enforcing correct usage,  but that would influence the actual test too much.
	 */
	interface WithF<T> {}
	
	/**
	 * Variant on WithF, where the type parameter is a TypeToken.
	 * TODO do we really need this?
	 */
	interface WithFToken<T extends TypeToken<?>> {}
	
	/**
	 * Uses the reflector being tested to get the type of the field named "f" in the given type.
	 * The returned value is cased into a TypeToken assuming the WithF interface is used correctly,
	 * and the reflector returned the correct result.
	 */
	@SuppressWarnings("unchecked") // assuming the WithT interface is used correctly
	private <T> TypeToken<? extends T> getF(TypeToken<? extends WithF<? extends T>> type) {
		return (TypeToken<? extends T>) getFieldType(type, "f");
	}
	/**
	 * Variant of {@link #getF(TypeToken)} that's stricter in arguments and return type, for checked equals tests.
	 * @see #getF(TypeToken)
	 */
	@SuppressWarnings("unchecked") // assuming the WithT interface is used correctly
	private <T> TypeToken<T> getStrictF(TypeToken<? extends WithF<T>> type) {
		return (TypeToken<T>) getFieldType(type, "f");
	}

	@SuppressWarnings("unchecked")
	private <T extends TypeToken<?>> T getFToken(TypeToken<? extends WithFToken<T>> type) {
		return (T) getFieldType(type, "f");
	}
	
	private TypeToken<?> getFieldType(Type forType, String fieldName) {
		try {
			Class<?> clazz = getClassType(forType);
			return TypeToken.get(strategy.getFieldType(forType, clazz.getField(fieldName)));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Error in test: can't find field " + fieldName, e);
		}
	}
	
//	private Type getReturnType(String methodName, Type forType) {
//		try {
//			Class<?> clazz = getClass(forType);
//			return strategy.getExactReturnType(clazz.getMethod(methodName), forType);
//		} catch (NoSuchMethodException e) {
//			throw new RuntimeException("Error in test: can't find method " + methodName, e);
//		}
//	}
	
	private <T, U extends T> void checkedTestExactSuperclassChain(TypeToken<T> type1, TypeToken<U> type2, TypeToken<? extends U> type3) {
		testExactSuperclassChain(type1, type2, type3);
	}
	
	private void testExactSuperclassChain(TypeToken<?> ... types) {
		for (int i = 0; i < types.length; i++) {
			assertTrue(isSupertype(types[i], types[i]));
			for (int j = i + 1; j < types.length; j++) {
				testExactSuperclass(types[i], types[j]);
			}
		}
	}

	private <T, U extends T> void checkedTestInexactSupertypeChain(TypeToken<T> type1, TypeToken<U> type2, TypeToken<? extends U> type3) {
		testInexactSupertypeChain(type1, type2, type3);
	}
	
	private void testInexactSupertypeChain(TypeToken<?> ...types) {
		for (int i = 0; i < types.length; i++) {
			assertTrue(isSupertype(types[i], types[i]));
			for (int j = i + 1; j < types.length; j++) {
				testInexactSupertype(types[i], types[j]);
			}
		}
	}
	
	/**
	 * Test that type1 is not a supertype of type2 (and, while we're at it, not vice-versa either).
	 */
	private void testNotSupertypes(TypeToken<?>... types) {
		for (int i = 0; i < types.length; i++) {
			for (int j = i + 1; j < types.length; j++) {
				assertFalse(isSupertype(types[i], types[j]));				
				assertFalse(isSupertype(types[j], types[i]));				
			}
		}
	}
	
	private <T> TypeToken<T> tt(Class<T> t) {
		return TypeToken.get(t);
	}
	
	public void testBasic() {
		checkedTestExactSuperclassChain(tt(Object.class), tt(Number.class), tt(Integer.class));
		testNotSupertypes(tt(Integer.class), tt(Double.class));
	}
	
	public void testSimpleTypeParam() {
		checkedTestExactSuperclassChain(COLLECTION_OF_STRING, LIST_OF_STRING, ARRAYLIST_OF_STRING);
		testNotSupertypes(COLLECTION_OF_STRING, new TypeToken<ArrayList<Integer>>(){});
	}

	public interface StringList extends List<String> {
	}

	public void testStringList() {
		checkedTestExactSuperclassChain(COLLECTION_OF_STRING, LIST_OF_STRING, tt(StringList.class));
	}
	
	public void testTextendsStringList() {
		class C<T extends StringList> implements WithF<T>{
			public T f;
		}

		// raw
		if (COMPILE_CHECK) {
			@SuppressWarnings("unchecked")
			C c = null;
			List<String> listOfString = c.f;
		}
		testExactSuperclass(LIST_OF_STRING, getFieldType(C.class, "f"));
		
		// wildcard
		TypeToken<? extends StringList> ft = getF(new TypeToken<C<?>>(){});
		checkedTestExactSuperclassChain(LIST_OF_STRING, tt(StringList.class), ft);
	}
	
	public void testExtendViaOtherTypeParam() {
		class C<T extends StringList, U extends T> implements WithF<U> {
			@SuppressWarnings("unused")
			public U f;
		}
		// raw
		testExactSuperclass(LIST_OF_STRING, getFieldType(C.class, "f"));
		// wildcard
		TypeToken<? extends StringList> ft = getF(new TypeToken<C<?,?>>(){});
		checkedTestExactSuperclassChain(LIST_OF_STRING, tt(StringList.class), ft);
	}
	
	@SuppressWarnings("unchecked")
	public void testMultiBoundParametrizedStringList() {
		class C<T extends Object & StringList> implements WithF<T>{
			@SuppressWarnings("unused")
			public T f;
		}
		// raw
		new C().f = new Object(); // compile check
		assertEquals(tt(Object.class), getFieldType(C.class, "f"));
		// wildcard
		TypeToken<? extends StringList> ft = getF(new TypeToken<C<?>>(){});
		checkedTestExactSuperclassChain(LIST_OF_STRING, tt(StringList.class), ft);
	}
	
	public void testFListOfT_String() {
		class C<T> implements WithF<List<T>> {
			@SuppressWarnings("unused")
			public List<T> f;
		}
		
		TypeToken<List<String>> ft = getStrictF(new TypeToken<C<String>>(){});
		assertCheckedTypeEquals(LIST_OF_STRING, ft);
	}

	public void testOfListOfString() {
		checkedTestExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, ARRAYLIST_OF_LIST_OF_STRING);
		testNotSupertypes(COLLECTION_OF_LIST_OF_STRING, new TypeToken<ArrayList<List<Integer>>>(){});
	}
	
	public void testFListOfListOfT_String() {
		class C<T> implements WithF<List<List<T>>> {
			@SuppressWarnings("unused")
			public List<List<T>> f;
		}
		TypeToken<List<List<String>>> ft = getStrictF(new TypeToken<C<String>>(){});
		assertCheckedTypeEquals(LIST_OF_LIST_OF_STRING, ft);
	}

	public interface ListOfListOfT<T> extends List<List<T>> {}
	
	public void testListOfListOfT_String() {
		checkedTestExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, new TypeToken<ListOfListOfT<String>>(){});
	}
	
	public interface ListOfListOfT_String extends ListOfListOfT<String> {}
	public void testListOfListOfT_StringInterface() {
		checkedTestExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, tt(ListOfListOfT_String.class));
	}
	
	public interface ListOfListOfString extends List<List<String>> {}
	public void testListOfListOfStringInterface() {
		checkedTestExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, tt(ListOfListOfString.class));
	}
	
	public void testWildcardTExtendsListOfListOfString() {
		class C<T extends List<List<String>>> implements WithF<T> {
			@SuppressWarnings("unused")
			public T f;
		}
		TypeToken<? extends List<List<String>>> ft = getF(new TypeToken<C<?>>(){});
		checkedTestExactSuperclass(COLLECTION_OF_LIST_OF_STRING, ft);
	}
	
	public void testExtWildcard() {
		checkedTestExactSuperclass(COLLECTION_OF_EXT_STRING, ARRAYLIST_OF_EXT_STRING);
		checkedTestExactSuperclass(COLLECTION_OF_LIST_OF_EXT_STRING, ARRAYLIST_OF_LIST_OF_EXT_STRING);
		testNotSupertypes(COLLECTION_OF_EXT_STRING, new TypeToken<ArrayList<Integer>>(){});
		testNotSupertypes(COLLECTION_OF_EXT_STRING, new TypeToken<ArrayList<Object>>(){});
	}
	
	public interface ListOfListOfExtT<T> extends List<List<? extends T>> {}
	public void testListOfListOfExtT_String() {
		checkedTestExactSuperclass(COLLECTION_OF_LIST_OF_EXT_STRING, new TypeToken<ListOfListOfExtT<String>>(){});
	}
	
	public void testUExtendsListOfExtT() {
		class C<T, U extends List<? extends T>> implements WithF<U> {
			@SuppressWarnings("unused")
			public U f;
		}
		
		// this doesn't compile in eclipse, so we hold the compilers hand by adding a step in between
		// TODO check if this compiles with sun compiler
		// TypeToken<? extends List<? extends String>> ft = getF(new TypeToken<C<? extends String, ?>>(){});
		TypeToken<? extends C<? extends String, ?>> tt = new TypeToken<C<? extends String, ?>>(){};
		TypeToken<? extends List<? extends String>> ft = getF(tt);

		checkedTestInexactSupertype(COLLECTION_OF_EXT_STRING, ft);
	}

	public void testListOfExtT() {
		class C<T> implements WithF<List<? extends T>> {
			@SuppressWarnings("unused")
			public List<? extends T> f;
		}
		TypeToken<? extends List<? extends String>> ft = getF(new TypeToken<C<String>>(){});
		checkedTestExactSuperclass(COLLECTION_OF_EXT_STRING, ft);
	}
	
	public void testListOfSuperT() {
		class C<T> implements WithF<List<? super T>> {
			@SuppressWarnings("unused")
			public List<? super T> f;
		}
		TypeToken<? extends List<? super String>> ft = getF(new TypeToken<C<String>>(){});
		checkedTestExactSuperclass(COLLECTION_OF_SUPER_STRING, ft);
	}
	
	public void testInnerFieldWithTypeOfOuter() {
		class Outer<T> {
			@SuppressWarnings("unused")
			class Inner implements WithF<T> {
				public T f;
			}
			class Inner2 implements WithF<List<List<? extends T>>> {
				@SuppressWarnings("unused")
				public List<List<? extends T>> f;
			}
		}
		
		TypeToken<String> ft = getStrictF(new TypeToken<Outer<String>.Inner>(){});
		assertCheckedTypeEquals(tt(String.class), ft);

		TypeToken<List<List<? extends String>>> ft2 = getStrictF(new TypeToken<Outer<String>.Inner2>(){});
		assertCheckedTypeEquals(LIST_OF_LIST_OF_EXT_STRING, ft2);
	}
	
	public void testInnerExtendsWithTypeOfOuter() {
		class Outer<T> {
			class Inner extends ArrayList<T> {
			}
		}
		checkedTestExactSuperclass(COLLECTION_OF_STRING, new TypeToken<Outer<String>.Inner>(){});
	}
	
	public void testInnerDifferentParams() {
		class Outer<T> {
			class Inner<S> {
			}
		}
		// inner param different
		testNotSupertypes(new TypeToken<Outer<String>.Inner<Integer>>(){}, new TypeToken<Outer<String>.Inner<String>>(){});
		// outer param different
		testNotSupertypes(new TypeToken<Outer<Integer>.Inner<String>>(){}, new TypeToken<Outer<String>.Inner<String>>(){});
	}

	/**
	 * Supertype of a raw type is erased
	 */
	@SuppressWarnings("unchecked")
	public void testSubclassRaw() {
		class Superclass<T extends Number> {
			public T t;
		}
		class Subclass<U> extends Superclass<Integer>{}
		assertEquals(tt(Number.class), getFieldType(Subclass.class, "t"));
		
		Number n = new Subclass().t; // compile check
		new Subclass().t = n; // compile check
	}

	/**
	 * Supertype of a raw type is erased.
	 * (And  there's no such thing as a ParameterizedType with some type parameters raw and others not)
	 */
	@SuppressWarnings("unchecked")
	public void testSubclassRawMix() {
		class Superclass<T, U extends Number> {
//			public T t;
			public U u;
		}
		class Subclass<T> extends Superclass<T, Integer> {}
		assertEquals(tt(Number.class), getFieldType(Subclass.class, "u"));
		
		Number n = new Subclass().u; // compile check
		new Subclass().u = n; // compile check
	}
	
	/**
	 * If a type has no parameters, it doesn't matter that it got erased.
	 * So even though Middleclass was erased, its supertype is not.
	 */
	public void testSubclassRawViaUnparameterized() {
		class Superclass<T extends Number> implements WithF<T> {
			@SuppressWarnings("unused")
			public T f;
		}
		class Middleclass extends Superclass<Integer> {}
		class Subclass<U> extends Middleclass {}
		
		TypeToken<Integer> ft = getStrictF(tt(Subclass.class));
		assertCheckedTypeEquals(tt(Integer.class), ft);
	}
	
	/**
	 * Similar for inner types: the outer type of a raw inner type is also erased
	 */
	@SuppressWarnings("unchecked")
	public void testInnerRaw() {
		class Outer<T extends Number> {
			public Inner rawInner;
			
			class Inner<U extends T> {
				public T t;
				public U u;
			}
		}
		
		assertEquals(tt(Outer.Inner.class), getFieldType(Outer.class, "rawInner"));
		assertEquals(tt(Number.class), getFieldType(Outer.Inner.class, "t"));
		assertEquals(tt(Number.class), getFieldType(Outer.Inner.class, "u"));
		
		if (COMPILE_CHECK) {
			Number n = new Outer<Integer>().rawInner.t; // compile check
			new Outer<Integer>().rawInner.t = n; // compile check
			n = new Outer<Integer>().rawInner.u; // compile check
			new Outer<Integer>().rawInner.u = n; // compile check
		}
	}
	
	public void testSuperWildcard() {
		Box<? super Integer> b = new Box<Integer>(); // compile check
		b.f = new Integer(0); // compile check
		
		testInexactSupertype(getFieldType(new TypeToken<Box<? super Integer>>(){}, "f"), tt(Integer.class));
		
		TypeToken<? super Integer> ft = getFToken(new TypeToken<Box<? super Integer>>(){});
		checkedTestInexactSupertype(ft, tt(Integer.class));
	}
	
	public void testContainment() {
		checkedTestInexactSupertypeChain(new TypeToken<List<?>>(){},
				new TypeToken<List<? extends Number>>(){},
				new TypeToken<List<Integer>>(){});
		checkedTestInexactSupertypeChain(new TypeToken<List<?>>(){},
				new TypeToken<List<? super Integer>>(){},
				new TypeToken<List<Object>>(){});
	}
	
	// TODO graph tests for recursively referring bounds
//	interface Graph<N extends Node<N, E>, E extends Edge<N, E>> {}
//	interface Node<N extends Node<N,E>, E extends Edge<N, E>> {}
//	interface Edge<N extends Node<N,E>, E extends Edge<N, E>> {}

}
