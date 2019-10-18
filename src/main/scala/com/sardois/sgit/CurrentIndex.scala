package com.sardois.sgit

import better.files.File

import scala.annotation.tailrec

case class CurrentIndex(repository: Repository, map: Map[String, String]) extends IO {

    override val file: File = repository.indexFile

    override def serialize: String = {
        map.keys.map( key => {
            key + " " + map(key)
        }).mkString(System.lineSeparator())
    }

    def add(relativePath: String, sha: String): CurrentIndex = {
        CurrentIndex(repository, map + (relativePath -> sha))
    }

    def addAll(list: List[(String, String)]): CurrentIndex = {
        @tailrec
        def rec(list: List[(String, String)], index: CurrentIndex): CurrentIndex = {
            list match {
                case ::(head, next) => {
                    val relativePath = head._1
                    val sha = head._2
                    rec(next, index.add(relativePath, sha))
                }
                case Nil => index
            }
        }

        rec(list, this)
    }

    def remove(relativePath: String): CurrentIndex = {
        val newMap = map.filter( tuple => {
            val key = tuple._1
            !(key == relativePath || File(key).isChildOf(File(relativePath)))
        })

        CurrentIndex(repository, newMap)
    }

    def removeAll(list: List[String]): CurrentIndex = {
        @tailrec
        def rec(list: List[String], index: CurrentIndex): CurrentIndex = {
            list match {
                case ::(head, next) => {
                    rec(next, index.remove(head))
                }
                case Nil => index
            }
        }

        rec(list, this)
    }
}

object CurrentIndex {

    def deserialize(repository: Repository, fileName: String, str: String): Either[String, CurrentIndex] = {
        val lines = str.linesIterator.toList
        val map = lines.map( line => {
            val split = line.split(" ")
            val path = split(0)
            val sha = split(1)
            (path -> sha)
        }).toMap

        Right(CurrentIndex(repository, map))
    }
}
