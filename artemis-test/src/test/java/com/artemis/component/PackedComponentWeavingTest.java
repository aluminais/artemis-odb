package com.artemis.component;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.STATIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.World;

public class PackedComponentWeavingTest {
	
	private World world;
	private Entity e1, e2;
	private TransPackedFloat packed;

	@Before
	public void setup() {
		world = new World();
		world.initialize();
		
		e1 = world.createEntity();
		packed = e1.createComponent(TransPackedFloat.class);
		e1.addToWorld();
		
		e2 = world.createEntity();
		packed = e2.createComponent(TransPackedFloat.class);
		e2.addToWorld();
	}
	
	@Test
	public void packed_component_has_sizeof() throws Exception {
		Field sizeOf = field("$_SIZE_OF");
		
		assertEquals(PRIVATE | STATIC | FINAL, sizeOf.getModifiers());
		assertEquals(int.class, sizeOf.getType());
		assertEquals(2, sizeOf.getInt(packed));
	}
	
	@Test @SuppressWarnings("static-method")
	public void packed_component_has_offset() throws Exception {
		Field offset = field("$offset");
		
		assertEquals(PRIVATE, offset.getModifiers());
		assertEquals(int.class, offset.getType());
	}
	
	@Test
	public void packed_component_has_backing_array() throws Exception {
		Field data = field("$data");
		
		assertEquals(PRIVATE | STATIC, data.getModifiers());
		assertEquals(float[].class, data.getType());
		assertEquals(64, ((float[])data.get(null)).length);
		
		Method grow = method("$grow");
		grow.invoke(packed);
		assertEquals(128, ((float[])data.get(null)).length);
	}
	
	@Test
	public void packed_component_updates_offset() throws Exception {
		assertEquals(0, getOffset(e1));
		assertNotEquals(getOffset(e1), getOffset(e2));
	}

	
	@Test 
	public void packed_component_replaces_field_access_with_backing_array() throws Exception {
		ComponentMapper<TransPackedFloat> mapper = world.getMapper(TransPackedFloat.class);
		mapper.get(e1).x(1).y(2);
		mapper.get(e2).x(3).y(4);
		
		assertEquals(1f, mapper.get(e1).x(), .001f);
		assertEquals(2f, mapper.get(e1).y(), .001f);
		assertEquals(3f, mapper.get(e2).x(), .001f);
		assertEquals(4f, mapper.get(e2).y(), .001f);
		
		try {
			TransPackedFloat.class.getDeclaredField("x");
			fail("Failed to remove field from component");
		} catch (Exception e) { /* expected */ }
		try {
			TransPackedFloat.class.getDeclaredField("y");
			fail("Failed to remove field from component");
		} catch (Exception e) { /* expected */ }
	}
	
	@Test
	public void packed_component_add_to_value() throws Exception {
		ComponentMapper<TransPackedFloat> mapper = world.getMapper(TransPackedFloat.class);
		mapper.get(e1).x(1).y(2);
		mapper.get(e2).x(3).y(4);
		
		mapper.get(e1).addX(4);
		mapper.get(e2).addX(8);
		
		assertEquals(5f, mapper.get(e1).x(), .001f);
		assertEquals(2f, mapper.get(e1).y(), .001f);
		assertEquals(11f, mapper.get(e2).x(), .001f);
		assertEquals(4f, mapper.get(e2).y(), .001f);
		
		try {
			TransPackedFloat.class.getDeclaredField("x");
			fail("Failed to remove field from component");
		} catch (Exception e) { /* expected */ }
		try {
			TransPackedFloat.class.getDeclaredField("y");
			fail("Failed to remove field from component");
		} catch (Exception e) { /* expected */ }
	}
	
	private int getOffset(Entity e) throws Exception {
		ComponentMapper<TransPackedFloat> mapper = world.getMapper(TransPackedFloat.class);
		return field("$offset").getInt(mapper.get(e));
	}
	
	private static Method method(String name) throws SecurityException, NoSuchMethodException {
		Method m = TransPackedFloat.class.getDeclaredMethod(name);
		assertNotNull(m);
		m.setAccessible(true);
		return m;
	}
	
	private static Field field(String name) throws NoSuchFieldException {
		Field f = TransPackedFloat.class.getDeclaredField(name);
		assertNotNull(f);
		f.setAccessible(true);
		return f;
	}
}
