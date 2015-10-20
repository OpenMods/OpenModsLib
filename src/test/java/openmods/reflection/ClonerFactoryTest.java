package openmods.reflection;

import openmods.reflection.ClonerFactory.ICloner;

import org.junit.Assert;
import org.junit.Test;

public class ClonerFactoryTest {

	public static class A {
		public int a;
		public double b;
		public String c;
	}

	public static class B extends A {}

	public static class C extends A {}

	@Test
	public void sameClassTest() {
		final ClonerFactory factory = new ClonerFactory();
		final ICloner<A> cloner = factory.getCloner(A.class);

		A from = new A();
		from.a = 1;
		from.b = 3.4;
		from.c = "hello";

		A to = new A();
		cloner.clone(from, to);
		Assert.assertEquals(from.a, to.a);
		Assert.assertEquals(from.b, to.b, 0);
		Assert.assertEquals(from.c, to.c);
	}

	@Test
	public void directDerrivativeTest() {
		final ClonerFactory factory = new ClonerFactory();
		final ICloner<A> cloner = factory.getCloner(A.class);

		A from = new A();
		from.a = 1;
		from.b = 3.4;
		from.c = "hello";

		B to = new B();

		cloner.clone(from, to);

		Assert.assertEquals(from.a, to.a);
		Assert.assertEquals(from.b, to.b, 0);
		Assert.assertEquals(from.c, to.c);
	}

	@Test
	public void commonParentTest() {
		final ClonerFactory factory = new ClonerFactory();
		final ICloner<A> cloner = factory.getCloner(A.class);

		B from = new B();
		from.a = 1;
		from.b = 3.4;
		from.c = "hello";

		C to = new C();

		cloner.clone(from, to);

		Assert.assertEquals(from.a, to.a);
		Assert.assertEquals(from.b, to.b, 0);
		Assert.assertEquals(from.c, to.c);
	}

	public static class Base {
		public int a;

		public int getBase() {
			return a;
		}

		public void setBase(int value) {
			this.a = value;
		}
	}

	public static class Override1 extends Base {
		public int a;

		public int getOverride1() {
			return a;
		}
	}

	public static class Override2 extends Override1 {
		public int a;

		public int getOverride2() {
			return a;
		}
	}

	@Test
	public void testOverridenField() {
		final ClonerFactory factory = new ClonerFactory();
		final ICloner<Override1> cloner = factory.getCloner(Override1.class);

		Override1 base = new Override1();
		base.setBase(5);
		base.a = 10;

		Override2 override = new Override2();
		override.a = 15;

		cloner.clone(base, override);

		Assert.assertEquals(5, override.getBase());
		Assert.assertEquals(10, override.getOverride1());
		Assert.assertEquals(15, override.getOverride2());
	}

	public static class Sub extends Base {
		public int a;
	}

	@Test
	public void testDerrivateToDerrivate() {
		final ClonerFactory factory = new ClonerFactory();
		final ICloner<Base> cloner = factory.getCloner(Base.class);

		Sub from = new Sub();
		from.a = 10;
		from.setBase(5);

		Sub to = new Sub();
		to.a = 4;

		cloner.clone(from, to);
		Assert.assertEquals(5, to.getBase());
		Assert.assertEquals(5, from.getBase());

		Assert.assertEquals(10, from.a);
		Assert.assertEquals(4, to.a);
	}

}
