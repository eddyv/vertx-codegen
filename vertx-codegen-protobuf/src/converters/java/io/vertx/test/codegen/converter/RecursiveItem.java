package io.vertx.test.codegen.converter;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.protobuf.annotations.ProtobufGen;

import java.util.Objects;

@DataObject
@ProtobufGen
public class RecursiveItem {

  private String id;
  private RecursiveItem childA;
  private RecursiveItem childB;
  private RecursiveItem childC;

  public RecursiveItem() {
  }

  public RecursiveItem(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public RecursiveItem getChildA() {
    return childA;
  }

  public void setChildA(RecursiveItem childA) {
    this.childA = childA;
  }

  public RecursiveItem getChildB() {
    return childB;
  }

  public void setChildB(RecursiveItem childB) {
    this.childB = childB;
  }

  public RecursiveItem getChildC() {
    return childC;
  }

  public void setChildC(RecursiveItem childC) {
    this.childC = childC;
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RecursiveItem that = (RecursiveItem) o;
    return Objects.equals(id, that.id) && Objects.equals(childA, that.childA) && Objects.equals(childB, that.childB) && Objects.equals(childC, that.childC);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, childA, childB, childC);
  }
}
