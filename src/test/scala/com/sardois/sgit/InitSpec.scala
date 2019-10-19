package com.sardois.sgit

import org.scalatest._

class InitSpec extends FlatSpec {

    "A initialized repository" should "be written on disk" in {
        val repo = IORepositoryTest.init()
        assert(repo.isDirectory())
        IORepositoryTest.delete(repo)
    }

    it should "create an indexes folder" in {
        val repo = IORepositoryTest.init()
        val indexFolder = repo/Repository.indexesPath
        assert(indexFolder.exists)
        assert(indexFolder.isDirectory)
        IORepositoryTest.delete(repo)
    }

    it should "create an empty index file" in {
        val repo = IORepositoryTest.init()
        val index = repo/Repository.indexPath
        assert(index.isRegularFile)
        val lines = index.contentAsString()
        assert(lines.isEmpty)
        IORepositoryTest.delete(repo)
    }

    it should "create an empty objects folder" in {
        val repo = IORepositoryTest.init()
        val blobs = repo/Repository.blobsPath
        assert(blobs.isDirectory)
        assert(blobs.isEmpty)
        IORepositoryTest.delete(repo)
    }

    it should "create a commits folder" in {
        val repo = IORepositoryTest.init()
        val commits = repo/Repository.commitsPath
        assert(commits.exists)
        assert(commits.isDirectory)
        IORepositoryTest.delete(repo)
    }

    it should "have a branches folder" in {
        val repo = IORepositoryTest.init()
        val branchFolder = repo/Repository.branchesPath
        assert(branchFolder.isDirectory())
        IORepositoryTest.delete(repo)
    }

    it should "have a tags folder" in {
        val repo = IORepositoryTest.init()
        val tagsFolder = repo/Repository.branchesPath
        assert(tagsFolder.isDirectory())
        IORepositoryTest.delete(repo)
    }

    it should "have it's master branch written" in {
        val repo = IORepositoryTest.init()
        val heads = repo/Repository.branchesPath
        val masterBranch = Branch.master
        val masterFile = heads/masterBranch.name
        val rootCommit = IOHead.getPreviousCommit(repo)

        // Check that the branch file exists, and
        // the pointed commit is the root commit.
        assert(masterFile.isRegularFile)
        assert(masterFile.contentAsString == rootCommit.sha)

        IORepositoryTest.delete(repo)
    }

    it should "have it's head set to master in the HEAD file" in {
        val repo = IORepositoryTest.init()
        val head = repo/Repository.headPath
        val text = head.lines().toList
        val master = Branch.master

        if (text.size != 1) fail("Invalid numbers of lines in the HEAD file")

        val split = text(0).split(" ")
        if (split.size != 2) fail("Invalid format in the HEAD file")

        val checkableType = split(0)
        val branchName = split(1)

        assert(checkableType == CheckableEnum.BRANCH.toString)
        assert(branchName == master.name)

        IORepositoryTest.delete(repo)
    }

    it should "have a checkables/tags empty folder" in {
        val repo = IORepositoryTest.init()
        val tags = repo/Repository.tagsPath
        assert(tags.isDirectory)
        assert(tags.isEmpty)
        IORepositoryTest.delete(repo)
    }

    it should "provide an error message if trying to be init inside a nested fodler of an sgit repo" in {
        val folder = Test.getRandomFolder
        IORepository.init(folder)

        val nestedDirs = folder/"nestedDir"/"anotherOne"
        nestedDirs.createDirectories()

        val either = IORepository.init(nestedDirs)
        IOTest.delete(folder)

        either match {
            case Right(_) => fail("Repository was still created")
            case _ => succeed
        }
    }

    it should "be destroyable for tests" in {
        val repo = IORepositoryTest.init()
        IORepositoryTest.delete(repo)
        assert(!repo.isDirectory())
    }
}