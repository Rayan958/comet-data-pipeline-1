package com.ebiznext.comet.schema.handlers

import com.ebiznext.comet.config.DatasetArea
import com.ebiznext.comet.schema.model._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.hadoop.fs.Path


/**
  * Handles access to datasets metadata,  eq. domains / types / schemas.
  *
  * @param storage : Underlying filesystem manager
  */
class SchemaHandler(storage: StorageHandler) {
  // uses Jackson YAML for parsing, relies on SnakeYAML for low level handling
  val mapper: ObjectMapper = new ObjectMapper(new YAMLFactory())
  mapper.registerModule(DefaultScalaModule)

  /**
    * All defined types.
    * Types are located in the only file "types.yml"
    */
  lazy val types: Types = {
    val typesPath = new Path(DatasetArea.types, "types.yml")
    mapper.readValue(storage.read(typesPath), classOf[Types])
  }

  /**
    * Fnd type by name
    *
    * @param tpe : Type name
    * @return Unique type referenced by this name.
    */
  def getType(tpe: String): Option[Type] = {
    types.types.find(_.name == tpe)
  }

  /**
    * All defined domains
    * Domains are defined under the "domains" folder in the metadata folder
    */
  lazy val domains: List[Domain] = {
    storage
      .list(DatasetArea.domains, ".yml")
      .map(path => mapper.readValue(storage.read(path), classOf[Domain]))

  }

  /**
    * All defined jobs
    * Jobs are defined under the "jobs" folder in the metadata folder
    */
  lazy val jobs: Map[String, AutoJobDesc] = {
    storage
      .list(DatasetArea.jobs, ".yml")
      .map(path => mapper.readValue(storage.read(path), classOf[AutoJobDesc]))
      .map(job => job.name -> job).toMap

  }

  /**
    * Find domain by name
    *
    * @param name : Domain name
    * @return Unique Domain referenced by this name.
    */
  def getDomain(name: String): Option[Domain] = {
    domains.find(_.name == name)
  }

  /**
    * Return all schemas for a domain
    *
    * @param domain : Domain name
    * @return List of schemas for a domain, empty list if no schema or domain is found
    */
  def getSchemas(domain: String): List[Schema] = {
    getDomain(domain).map(_.schemas).getOrElse(Nil)
  }

  /**
    * Get schema by name for a domain
    *
    * @param domainName : Domain name
    * @param schemaName : Sceham name
    * @return Unique Schema with this name for a domain
    */
  def getSchema(domainName: String, schemaName: String): Option[Schema] = {
    for {
      domain <- getDomain(domainName)
      schema <- domain.schemas.find(_.name == schemaName)
    } yield schema
  }
}
