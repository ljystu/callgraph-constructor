package p;

import calls.B;

public class C {

	public void c1(B b) {
		b.b1();
	}

	public void c2() {
		c1(null);
	}
}