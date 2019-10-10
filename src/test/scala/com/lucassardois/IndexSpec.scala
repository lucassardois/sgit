package com.lucassardois

import org.scalatest._
import better.files._
import com.lucassardois.IOIndex.read

class IndexSpec extends FlatSpec {

    "A repository index" should "be able to add files" in {
        val repo = IORepositoryTest.init()
        val file = Test.createRandomFile(repo.parent)

        val error = IOIndex.add(
            repo,
            repo.parent,
            Config(paths = List(file.pathAsString))
        )

        error match {
            case Some(error) => fail(error)
            case _ =>
        }

        val index = repo/Repository.getIndexPath()
        if (!index.exists) {
            fail("The index doesn't exists")
        }

        val lines = index.lines().toList
        if (lines.size < 1) {
            fail("The number of entries in the index file is incorrect")
        }

        val entry = lines(0)
        val split = entry.split(" ")
        if (split.size < 2) {
            fail("Invalid entry in the index")
        }

        val path = split(0)
        val sha = split(1)

        assert(path == Repository.relativePathFromRepo(repo, file))
        assert(sha == file.sha256)

        IORepositoryTest.delete(repo)
    }

    it should "returns an error when adding non existing files" in {
        val repo = IORepositoryTest.init()
        val file = Test.createRandomFile(repo.parent)
        val filePath = file.pathAsString
        file.delete()

        val error = IOIndex.add(
            repo,
            repo.parent,
            Config(paths = List(filePath))
        )

        error match {
            case Some(error) => succeed
            case _ => fail("No error message returned...")
        }

        IORepositoryTest.delete(repo)
    }

    it should "be readable" in {
        val repo = IORepositoryTest.init()
        val dir = repo.parent
        val files = List(
            Test.createRandomFile(dir),
            Test.createRandomFile(dir),
            Test.createRandomFile(dir)
        )

        val filesPath = files.map( (file) => {
            file.pathAsString
        })

        val error = IOIndex.add(
            repo,
            repo.parent,
            Config(paths = filesPath)
        )

        error match {
            case Some(error) => fail(error)
            case _ =>
        }

        val either = IOIndex.read(repo)

        either match {
            case Left(error) => fail(error)
            case Right(mapIndex) => {
                val file = repo/Repository.getIndexPath()

                if (!file.isRegularFile) {
                    fail("Index file is invalid.")
                }

                assert(mapIndex.size == files.size)
            }
        }

        IORepositoryTest.delete(repo)
    }

    it should "be able to remove previously added files" in {
        val repo = IORepositoryTest.init()
        val file = Test.createRandomFile(repo.parent)

       IOIndex.add(
            repo,
            repo.parent,
            Config(paths = List(file.pathAsString))
       ) match {
           case Some(error) => fail(error)
           case _ =>
       }

        IOIndex.remove(
            repo,
            repo.parent,
            Config(paths = List(file.pathAsString))
        ) match {
            case Some(error) => fail(error)
            case _ =>
        }

        val index = repo/Repository.getIndexPath()
        if (!index.exists) {
            fail("The index doesn't exists")
        }

        val lines = index.lines().toList
        assert(lines.size == 0)

        IORepositoryTest.delete(repo)
    }

    it should "returns an error when removing non tracked files" in {
        val repo = IORepositoryTest.init()
        val file = Test.createRandomFile(repo.parent)

        IOIndex.remove(
            repo,
            repo.parent,
            Config(paths = List(file.pathAsString))
        ) match {
            case Some(error) => succeed
            case _ => fail("Files where still added")
        }

        IORepositoryTest.delete(repo)
    }

    it should "not stage directories, .sgit folder and the parent repository folder" in {
        val repo = IORepositoryTest.init()
        val dir = (repo.parent/"dir").createDirectories()
        val nested = (dir/"nested").createDirectories()

        Test.createRandomFile(repo)
        Test.createRandomFile(repo.parent)
        Test.createRandomFile(dir)
        Test.createRandomFile(nested)

        val files = Repository.list(repo)
        StagedFile.createAllFromFiles(repo, files) match {
            case Left(error) => fail(error)
            case Right(stagedFiles) => {
               assert(stagedFiles.size == 3)
            }
        }

        IORepositoryTest.delete(repo)
    }

    it should "returns the list of untracked files" in {
        val repo = IORepositoryTest.init()
        val dir = (repo.parent/"dir").createDirectories()
        Test.createRandomFile(repo.parent)
        Test.createRandomFile(dir)

        val mapIndex = Map[String, String]()
        val files = Repository.list(repo)

        StagedFile.createAllFromFiles(repo, files) match {
            case Left(error) => fail(error)
            case Right(stagedFiles) => {
                val untrackedFiles = Index.listUntrackedFiles(mapIndex, stagedFiles)
                assert(untrackedFiles.size == 2)
            }
        }

        IORepositoryTest.delete(repo)
    }
}