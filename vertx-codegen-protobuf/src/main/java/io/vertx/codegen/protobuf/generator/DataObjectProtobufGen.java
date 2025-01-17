package io.vertx.codegen.protobuf.generator;

import io.vertx.codegen.DataObjectModel;
import io.vertx.codegen.Generator;
import io.vertx.codegen.PropertyInfo;
import io.vertx.codegen.protobuf.annotations.JsonProtoEncoding;
import io.vertx.codegen.protobuf.annotations.ProtobufGen;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.writer.CodeWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DataObjectProtobufGen extends Generator<DataObjectModel> {

  public static int CACHE_INITIAL_CAPACITY = 16;

  public DataObjectProtobufGen() {
    kinds = Collections.singleton("dataObject");
    name = "data_object_converters";
  }

  @Override
  public Collection<Class<? extends Annotation>> annotations() {
    return Collections.singletonList(ProtobufGen.class);
  }

  @Override
  public String filename(DataObjectModel model) {
    if (model.isClass() && model.getAnnotations().stream().anyMatch(ann -> ann.getName().equals(ProtobufGen.class.getName()))) {
      return model.getFqn() + "ProtoConverter.java";
    }
    return null;
  }

  @Override
  public String render(DataObjectModel model, int index, int size, Map<String, Object> session) {
    return renderProto(model, index, size, session);
  }

  public String renderProto(DataObjectModel model, int index, int size, Map<String, Object> session) {
    StringWriter buffer = new StringWriter();
    PrintWriter writer = new PrintWriter(buffer);
    CodeWriter code = new CodeWriter(writer);
    String visibility = model.isPublicConverter() ? "public" : "";

    JsonProtoEncoding jsonProtoEncoding = JsonProtoEncodingSelector.select(model);

    writer.print("package " + model.getType().getPackageName() + ";\n");
    writer.print("\n");
    writer.print("import com.google.protobuf.CodedOutputStream;\n");
    writer.print("import com.google.protobuf.CodedInputStream;\n");
    writer.print("import java.io.IOException;\n");
    writer.print("import java.time.Instant;\n");
    writer.print("import java.time.ZonedDateTime;\n");
    writer.print("import java.util.ArrayList;\n");
    writer.print("import java.util.List;\n");
    writer.print("import java.util.HashMap;\n");
    writer.print("import java.util.Map;\n");
    writer.print("import java.util.Arrays;\n");
    writer.print("import io.vertx.core.json.JsonObject;\n");
    writer.print("import io.vertx.codegen.protobuf.utils.ExpandableIntArray;\n");
    writer.print("import io.vertx.codegen.protobuf.converters.*;\n");
    writer.print("\n");
    code
      .codeln("public class " + model.getType().getSimpleName() + "ProtoConverter {"
      ).newLine();

    String simpleName = model.getType().getSimpleName();

    // fromProto()
    {
      writer.print("  " + visibility + " static void fromProto(CodedInputStream input, " + simpleName + " obj) throws IOException {\n");
      writer.print("    int tag;\n");
      writer.print("    while ((tag = input.readTag()) != 0) {\n");
      writer.print("      switch (tag) {\n");
      int fieldNumber = 1;
      for (PropertyInfo prop : model.getPropertyMap().values()) {
        ClassKind propKind = prop.getType().getKind();
        ProtoProperty protoProperty = ProtoProperty.getProtoProperty(prop, fieldNumber);
        writer.print("        case " + protoProperty.getTag() + ": {\n");
        if (prop.getKind().isList()) {
          if (propKind.basic) {
            writer.print("          int length = input.readRawVarint32();\n");
            writer.print("          int limit = input.pushLimit(length);\n");
            writer.print("          List<Integer> list = new ArrayList<>();\n");
            writer.print("          while (input.getBytesUntilLimit() > 0) {\n");
            writer.print("            list.add(input." + protoProperty.getProtoType().read() + "());\n");
            writer.print("          }\n");
            writer.print("          obj." + prop.getSetterMethod() + "(list);\n");
            writer.print("          input.popLimit(limit);\n");
            writer.print("          break;\n");
          } else {
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("          int length = input.readUInt32();\n");
              writer.print("          int limit = input.pushLimit(length);\n");
              writer.print("          if (obj." + prop.getGetterMethod() + "() == null) {\n");
              writer.print("            obj." + prop.getSetterMethod() + "(new ArrayList<>());\n");
              writer.print("          }\n");
              writer.print("          obj." + prop.getGetterMethod() + "().add(" + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".fromProto(input));\n");
              writer.print("          input.popLimit(limit);\n");
              writer.print("          break;\n");
            } else {
              writer.print("          int length = input.readUInt32();\n");
              writer.print("          int limit = input.pushLimit(length);\n");
              writer.print("          " + protoProperty.getMessage() + " nested = new " + protoProperty.getMessage() + "();\n");
              writer.print("          " + protoProperty.getMessage() + "ProtoConverter.fromProto(input, nested);\n");
              writer.print("          if (obj." + prop.getGetterMethod() + "() == null) {\n");
              writer.print("            obj." + prop.getSetterMethod() + "(new ArrayList<>());\n");
              writer.print("          }\n");
              writer.print("          obj." + prop.getGetterMethod() + "().add(nested);\n");
              writer.print("          input.popLimit(limit);\n");
              writer.print("          break;\n");
            }
          }
        } else if (prop.getKind().isMap()) {
          if (propKind.basic) {
            writer.print("          int length = input.readRawVarint32();\n");
            writer.print("          int limit = input.pushLimit(length);\n");
            writer.print("          Map<String, " + prop.getType().getSimpleName() + "> map = obj." + prop.getGetterMethod() + "();\n");
            writer.print("          if (map == null) {\n");
            writer.print("            map = new HashMap<>();\n");
            writer.print("          }\n");
            writer.print("          input.readTag();\n");
            writer.print("          String key = input.readString();\n");
            writer.print("          input.readTag();\n");
            writer.print("          " + prop.getType().getSimpleName() + " value = input." + protoProperty.getProtoType().read() + "();\n");
            writer.print("          map.put(key, value);\n");
            writer.print("          obj." + prop.getSetterMethod() + "(map);\n");
            writer.print("          input.popLimit(limit);\n");
            writer.print("          break;\n");
          } else {
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("          int length = input.readUInt32();\n");
              writer.print("          int limit = input.pushLimit(length);\n");
              writer.print("          Map<String, " + builtInType + "> map = obj." + prop.getGetterMethod() + "();\n");
              writer.print("          if (map == null) {\n");
              writer.print("            map = new HashMap<>();\n");
              writer.print("          }\n");
              writer.print("          input.readTag();\n");
              writer.print("          String key = input.readString();\n");
              writer.print("          input.readTag();\n");
              writer.print("          int vlength = input.readUInt32();\n");
              writer.print("          int vlimit = input.pushLimit(vlength);\n");
              writer.print("          map.put(key, " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".fromProto(input));\n");
              writer.print("          obj." + prop.getSetterMethod() + "(map);\n");
              writer.print("          input.popLimit(vlimit);\n");
              writer.print("          input.popLimit(limit);\n");
              writer.print("          break;\n");
            } else {
              writer.print("          int length = input.readUInt32();\n");
              writer.print("          int limit = input.pushLimit(length);\n");
              writer.print("          Map<String, " + protoProperty.getMessage() + "> map = obj." + prop.getGetterMethod() + "();\n");
              writer.print("          if (map == null) {\n");
              writer.print("            map = new HashMap<>();\n");
              writer.print("          }\n");
              writer.print("          input.readTag();\n");
              writer.print("          String key = input.readString();\n");
              writer.print("          input.readTag();\n");
              writer.print("          int vlength = input.readUInt32();\n");
              writer.print("          int vlimit = input.pushLimit(vlength);\n");
              writer.print("          " + protoProperty.getMessage() + " value = new " + protoProperty.getMessage() + "();\n");
              writer.print("          " + protoProperty.getMessage() + "ProtoConverter.fromProto(input, value);\n");
              writer.print("          map.put(key, value);\n");
              writer.print("          obj." + prop.getSetterMethod() + "(map);\n");
              writer.print("          input.popLimit(vlimit);\n");
              writer.print("          input.popLimit(limit);\n");
              writer.print("          break;\n");
            }
          }
        } else {
          if (propKind.basic) {
            String javaDataType = prop.getType().getName();
            String casting = "";
            if ("java.lang.Short".equals(javaDataType) || "short".equals(javaDataType)) {
              casting = "(short) ";
            } else if ("java.lang.Character".equals(javaDataType) || "char".equals(javaDataType)) {
              casting = "(char) ";
            } else if ("java.lang.Byte".equals(javaDataType) || "byte".equals(javaDataType)) {
              casting = "(byte) ";
            }
            writer.print("          obj." + prop.getSetterMethod() + "(" + casting + "input." + protoProperty.getProtoType().read() + "());\n");
          } else {
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("          int length = input.readUInt32();\n");
              writer.print("          int limit = input.pushLimit(length);\n");
              writer.print("          obj." + prop.getSetterMethod() + "(" + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".fromProto(input));\n");
              writer.print("          input.popLimit(limit);\n");
            } else {
              writer.print("          int length = input.readUInt32();\n");
              writer.print("          int limit = input.pushLimit(length);\n");
              writer.print("          " + protoProperty.getMessage() + " nested = new " + protoProperty.getMessage() + "();\n");
              writer.print("          " + protoProperty.getMessage() + "ProtoConverter.fromProto(input, nested);\n");
              writer.print("          obj." + prop.getSetterMethod() + "(nested);\n");
              writer.print("          input.popLimit(limit);\n");
            }
          }
          writer.print("          break;\n");
        }
        writer.print("        }\n");
        fieldNumber++;
      }
      writer.print("      }\n");
      writer.print("    }\n");
      writer.print("  }\n");
      writer.print("\n");
    }

    // toProto()
    {
      writer.print("  public static void toProto(" + simpleName + " obj, CodedOutputStream output) throws IOException {\n");
      writer.print("    ExpandableIntArray cache = new ExpandableIntArray(" + CACHE_INITIAL_CAPACITY + ");\n");
      writer.print("    " + simpleName + "ProtoConverter.computeSize(obj, cache, 0);\n");
      writer.print("    " + simpleName + "ProtoConverter.toProto(obj, output, cache, 0);\n");
      writer.print("  }\n");
      writer.print("\n");
      writer.print("  " + visibility + " static int toProto(" + simpleName + " obj, CodedOutputStream output, ExpandableIntArray cache, int index) throws IOException {\n");
      writer.print("    index = index + 1;\n");
      int fieldNumber = 1;
      for (PropertyInfo prop : model.getPropertyMap().values()) {
        ClassKind propKind = prop.getType().getKind();
        ProtoProperty protoProperty = ProtoProperty.getProtoProperty(prop, fieldNumber);
        if (protoProperty.isNullable()) {
          writer.print("    if (obj." + prop.getGetterMethod() + "() != null) {\n");
        } else {
          if ("boolean".equals(prop.getType().getName())) {
            writer.print("    if (obj." + prop.getGetterMethod() + "()) {\n");
          } else {
            writer.print("    if (obj." + prop.getGetterMethod() + "() != 0) {\n");
          }
        }
        if (prop.getKind().isList()) {
          if (propKind.basic) {
            writer.print("      // list | tag | data size | value[0] | value[1] | value[2] |\n");
            writer.print("      if (obj." + prop.getGetterMethod() + "().size() > 0) {\n");
            writer.print("        output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
            writer.print("        int dataSize = 0;\n");
            writer.print("        for (Integer element: obj." + prop.getGetterMethod() + "()) {\n");
            writer.print("          dataSize += CodedOutputStream.computeInt32SizeNoTag(element);\n");
            writer.print("        }\n");
            writer.print("        output.writeUInt32NoTag(dataSize);\n");
            writer.print("        for (Integer element: obj." + prop.getGetterMethod() + "()) {\n");
            writer.print("          output." + protoProperty.getProtoType().writeNoTag() + "(element);\n");
            writer.print("        }\n");
            writer.print("      }\n");
          } else {
            writer.print("      // list[0] | tag | data size | value |\n");
            writer.print("      // list[1] | tag | data size | value |\n");
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("      for (" + protoProperty.getMessage() + " element: obj." + prop.getGetterMethod() +"()) {\n");
              writer.print("        output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
              writer.print("        output.writeUInt32NoTag(" + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".computeSize(element));\n");
              writer.print("        " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".toProto(element, output);\n");
              writer.print("      }\n");
            } else {
              writer.print("      for (" + protoProperty.getMessage() + " element: obj." + prop.getGetterMethod() +"()) {\n");
              writer.print("        output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
              writer.print("        output.writeUInt32NoTag(cache.get(index));\n");
              writer.print("        index = " + protoProperty.getMessage() + "ProtoConverter.toProto(element, output, cache, index);\n");
              writer.print("      }\n");
            }
          }
        } else if (prop.getKind().isMap()) {
          if (propKind.basic) {
            writer.print("      // map[0] | tag | data size | key | value |\n");
            writer.print("      // map[1] | tag | data size | key | value |\n");
            writer.print("      for (Map.Entry<String, " + prop.getType().getSimpleName() + "> entry : obj." + prop.getGetterMethod() + "().entrySet()) {\n");
            writer.print("        output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
            writer.print("        // calculate data size\n");
            writer.print("        int dataSize = 0;\n");
            writer.print("        dataSize += CodedOutputStream.computeStringSize(1, entry.getKey());\n");
            writer.print("        dataSize += CodedOutputStream." + protoProperty.getProtoType().computeSize() + "(2, entry.getValue());\n");
            writer.print("        // key\n");
            writer.print("        output.writeUInt32NoTag(dataSize);\n");
            writer.print("        // value\n");
            writer.print("        output.writeString(1, entry.getKey());\n");
            writer.print("        output." + protoProperty.getProtoType().write() + "(2, entry.getValue());\n");
            writer.print("      }\n");
          } else {
            writer.print("      // map[0] | tag | data size | key | value |\n");
            writer.print("      // map[1] | tag | data size | key | value |\n");
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("      for (Map.Entry<String, " + builtInType + "> entry : obj." + prop.getGetterMethod() + "().entrySet()) {\n");
              writer.print("        output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
              writer.print("        // calculate data size\n");
              writer.print("        int elementSize = " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".computeSize(entry.getValue());\n");
              writer.print("        int dataSize = 0;\n");
              writer.print("        dataSize += CodedOutputStream.computeStringSize(1, entry.getKey());\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(18);\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(elementSize);\n");
              writer.print("        dataSize += elementSize;\n");
              writer.print("        // key\n");
              writer.print("        output.writeUInt32NoTag(dataSize);\n");
              writer.print("        // value\n");
              writer.print("        output.writeString(1, entry.getKey());\n");
              writer.print("        output.writeUInt32NoTag(18);\n");
              writer.print("        output.writeUInt32NoTag(elementSize);\n");
              writer.print("        " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".toProto(entry.getValue(), output);\n");
              writer.print("      }\n");
            } else {
              writer.print("      for (Map.Entry<String, " + protoProperty.getMessage() + "> entry : obj." + prop.getGetterMethod() + "().entrySet()) {\n");
              writer.print("        output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
              writer.print("        // calculate data size\n");
              writer.print("        int elementSize = cache.get(index);\n");
              writer.print("        int dataSize = 0;\n");
              writer.print("        dataSize += CodedOutputStream.computeStringSize(1, entry.getKey());\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(18);\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(elementSize);\n");
              writer.print("        dataSize += elementSize;\n");
              writer.print("        // key\n");
              writer.print("        output.writeUInt32NoTag(dataSize);\n");
              writer.print("        // value\n");
              writer.print("        output.writeString(1, entry.getKey());\n");
              writer.print("        output.writeUInt32NoTag(18);\n");
              writer.print("        output.writeUInt32NoTag(elementSize);\n");
              writer.print("        index = " + protoProperty.getMessage() + "ProtoConverter.toProto(entry.getValue(), output, cache, index);\n");
              writer.print("      }\n");
            }
          }
        } else {
          if (propKind.basic) {
            writer.print("      output." + protoProperty.getProtoType().write() + "(" + fieldNumber + ", obj." + prop.getGetterMethod() + "());\n");
          } else {
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("      output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
              writer.print("      output.writeUInt32NoTag(" + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".computeSize(obj." + prop.getGetterMethod() + "()));\n");
              writer.print("      " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".toProto(obj." + prop.getGetterMethod() +"(), output);\n");
            } else {
              writer.print("      output.writeUInt32NoTag(" + protoProperty.getTag() + ");\n");
              writer.print("      output.writeUInt32NoTag(cache.get(index));\n");
              writer.print("      index = " + protoProperty.getMessage() + "ProtoConverter.toProto(obj." + prop.getGetterMethod() + "(), output, cache, index);\n");
            }
          }
        }
        writer.print("    }\n");
        fieldNumber++;
      }
      writer.print("    return index;\n");
      writer.print("  }\n");
      writer.print("\n");
    }

    // computeSize()
    {
      writer.print("  " + visibility + " static int computeSize(" + simpleName + " obj) {\n");
      writer.print("    ExpandableIntArray cache = new ExpandableIntArray(" + CACHE_INITIAL_CAPACITY + ");\n");
      writer.print("    " + simpleName + "ProtoConverter.computeSize(obj, cache, 0);\n");
      writer.print("    return cache.get(0);\n");
      writer.print("  }\n");
      writer.print("\n");
      writer.print("  " + visibility + " static int computeSize(" + simpleName + " obj, ExpandableIntArray cache, final int baseIndex) {\n");
      writer.print("    int size = 0;\n");
      writer.print("    int index = baseIndex + 1;\n");
      int fieldNumber = 1;
      for (PropertyInfo prop : model.getPropertyMap().values()) {
        ClassKind propKind = prop.getType().getKind();
        ProtoProperty protoProperty = ProtoProperty.getProtoProperty(prop, fieldNumber);
        if (protoProperty.isNullable()) {
          writer.print("    if (obj." + prop.getGetterMethod() + "() != null) {\n");
        } else {
          if ("boolean".equals(prop.getType().getName())) {
            writer.print("    if (obj." + prop.getGetterMethod() + "()) {\n");
          } else {
            writer.print("    if (obj." + prop.getGetterMethod() + "() != 0) {\n");
          }
        }
        if (prop.getKind().isList()) {
          if (propKind.basic) {
            writer.print("      // list | tag | data size | value[0] | value[1] | value[2] |\n");
            writer.print("      if (obj." + prop.getGetterMethod() + "().size() > 0) {\n");
            writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
            writer.print("        int dataSize = 0;\n");
            writer.print("        for (Integer element: obj." + prop.getGetterMethod() + "()) {\n");
            writer.print("          dataSize += CodedOutputStream." + protoProperty.getProtoType().computeSizeNoTag() + "(element);\n");
            writer.print("        }\n");
            writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
            writer.print("        size += dataSize;\n");
            writer.print("      }\n");
          } else {
            writer.print("      // list[0] | tag | data size | value |\n");
            writer.print("      // list[1] | tag | data size | value |\n");
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("      if (obj." + prop.getGetterMethod() + "().size() > 0) {\n");
              writer.print("        for (" + builtInType + " element: obj." + prop.getGetterMethod() + "()) {\n");
              writer.print("          size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
              writer.print("          int dataSize = " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".computeSize(element);\n");
              writer.print("          size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
              writer.print("          size += dataSize;\n");
              writer.print("        }\n");
              writer.print("      }\n");
            } else {
              writer.print("      if (obj." + prop.getGetterMethod() + "().size() > 0) {\n");
              writer.print("        for (" + protoProperty.getMessage() + " element: obj." + prop.getGetterMethod() + "()) {\n");
              writer.print("          size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
              writer.print("          int savedIndex = index;\n");
              writer.print("          index = " + protoProperty.getMessage() + "ProtoConverter.computeSize(element, cache, index);\n");
              writer.print("          int dataSize = cache.get(savedIndex);\n");
              writer.print("          size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
              writer.print("          size += dataSize;\n");
              writer.print("        }\n");
              writer.print("      }\n");
            }
          }
        } else if (prop.getKind().isMap()) {
          if (propKind.basic) {
            writer.print("      // map[0] | tag | data size | key | value |\n");
            writer.print("      // map[1] | tag | data size | key | value |\n");
            writer.print("      for (Map.Entry<String, " + prop.getType().getSimpleName() + "> entry : obj." + prop.getGetterMethod() + "().entrySet()) {\n");
            writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
            writer.print("        int dataSize = 0;\n");
            writer.print("        dataSize += CodedOutputStream.computeStringSize(1, entry.getKey());\n");
            writer.print("        dataSize += CodedOutputStream." + protoProperty.getProtoType().computeSize() + "(2, entry.getValue());\n");
            writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
            writer.print("        size += dataSize;\n");
            writer.print("      }\n");
          } else {
            writer.print("        // map[0] | tag | data size | key | value |\n");
            writer.print("        // map[1] | tag | data size | key | value |\n");
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("      for (Map.Entry<String, " + builtInType + "> entry : obj." + prop.getGetterMethod() + "().entrySet()) {\n");
              writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
              writer.print("        // calculate data size\n");
              writer.print("        int dataSize = 0;\n");
              writer.print("        // key\n");
              writer.print("        dataSize += CodedOutputStream.computeStringSize(1, entry.getKey());\n");
              writer.print("        // value\n");
              writer.print("        int elementSize = " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".computeSize(entry.getValue());\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(18);\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(elementSize);\n");
              writer.print("        dataSize += elementSize;\n");
              writer.print("        // data size\n");
              writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
              writer.print("        size += dataSize;\n");
              writer.print("      }\n");
            } else {
              writer.print("      for (Map.Entry<String, " + protoProperty.getMessage() + "> entry : obj." + prop.getGetterMethod() + "().entrySet()) {\n");
              writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
              writer.print("        // calculate data size\n");
              writer.print("        int dataSize = 0;\n");
              writer.print("        // key\n");
              writer.print("        dataSize += CodedOutputStream.computeStringSize(1, entry.getKey());\n");
              writer.print("        // value\n");
              writer.print("        int savedIndex = index;\n");
              writer.print("        index = " + protoProperty.getMessage() + "ProtoConverter.computeSize(entry.getValue(), cache, index);\n");
              writer.print("        int elementSize = cache.get(savedIndex);\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(18);\n");
              writer.print("        dataSize += CodedOutputStream.computeInt32SizeNoTag(elementSize);\n");
              writer.print("        dataSize += elementSize;\n");
              writer.print("        // data size\n");
              writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
              writer.print("        size += dataSize;\n");
              writer.print("      }\n");
            }
          }
        } else {
          if (propKind.basic) {
            writer.print("      size += CodedOutputStream." + protoProperty.getProtoType().computeSize() + "(" + fieldNumber + ", obj." + prop.getGetterMethod() + "());\n");
          } else {
            if (protoProperty.isBuiltinType()) {
              String builtInType = prop.getType().getSimpleName();
              writer.print("      size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
              writer.print("      int dataSize = " + ProtoProperty.getBuiltInProtoConverter(builtInType, jsonProtoEncoding) + ".computeSize(obj." + prop.getGetterMethod() + "());\n");
              writer.print("      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
              writer.print("      size += dataSize;\n");
            } else {
              writer.print("      size += CodedOutputStream.computeUInt32SizeNoTag(" + protoProperty.getTag() + ");\n");
              writer.print("      int savedIndex = index;\n");
              writer.print("      index = " + protoProperty.getMessage() + "ProtoConverter.computeSize(obj." + prop.getGetterMethod() + "(), cache, index);\n");
              writer.print("      int dataSize = cache.get(savedIndex);\n");
              writer.print("      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
              writer.print("      size += dataSize;\n");
            }
          }
        }
        writer.print("    }\n");
        fieldNumber++;
      }
      writer.print("    cache.set(baseIndex, size);\n");
      writer.print("    return index;\n");
      writer.print("  }\n");
      writer.print("\n");
      writer.print("}\n");
    }

    return buffer.toString();
  }
}
