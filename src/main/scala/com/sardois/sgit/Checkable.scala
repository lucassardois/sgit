package com.sardois.sgit

import better.files.File

case class Branch(repository: Repository, name: String, commitSha: String) extends IO {

    @impure
    lazy val commit: Either[String, Commit] = {
        val rootCommit = Commit.root(repository)
        if (commitSha == rootCommit.sha) {
            Right(rootCommit)
        } else {
            val commitFile = repository.commitsFolder/commitSha
            IO.read(repository, commitFile, Commit.deserialize)
        }
    }

    override val file: File = repository.branchesFolder/name

    @impure
    override def serialize: String = commitSha
}

object Branch {

    def deserialize(repository: Repository, fileName: String, str: String): Either[String, Branch] = {
        Right(Branch(repository, fileName, str))
    }
}

case class Tag(repository: Repository, name: String, commitSha: String) extends IO {

    @impure
    lazy val commit: Either[String, Commit] = {
        val rootCommit = Commit.root(repository)
        if (commitSha == rootCommit.sha) {
            Right(rootCommit)
        } else {
            val commitFile = repository.commitsFolder/commitSha
            IO.read(repository, commitFile, Commit.deserialize)
        }
    }

    override val file: File = repository.tagsFolder/name

    @impure
    override def serialize: String = commitSha
}

object Tag {

    def deserialize(repository: Repository, fileName: String, str: String): Either[String, Tag] = {
        Right(Tag(repository, fileName, str))
    }
}



