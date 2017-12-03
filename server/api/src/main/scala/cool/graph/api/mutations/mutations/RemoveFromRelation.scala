package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.mutactions.RemoveDataItemFromRelationByToAndFromField
import cool.graph.api.database.mutactions.{ClientSqlMutaction, Mutaction, MutactionGroup, Transaction}
import cool.graph.api.mutations.definitions.RemoveFromRelationDefinition
import cool.graph.api.mutations.{ClientMutation, ReturnValueResult}
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models._
import sangria.schema
import scaldi._

import scala.concurrent.Future

class RemoveFromRelation(relation: Relation, fromModel: Model, project: Project, args: schema.Args, dataResolver: DataResolver)(
    implicit apiDependencies: ApiDependencies)
    extends ClientMutation(fromModel, args, dataResolver) {

  override val mutationDefinition = RemoveFromRelationDefinition(relation, project)

  var aId: Id = extractIdFromScalarArgumentValues_!(args, mutationDefinition.bName)

  def prepareMutactions(): Future[List[MutactionGroup]] = {

    val aField = relation.getModelAField_!(project)
    val bField = relation.getModelBField_!(project)

    val bId = extractIdFromScalarArgumentValues_!(args, mutationDefinition.aName)

    var sqlMutactions = List[ClientSqlMutaction]()

    sqlMutactions :+=
      RemoveDataItemFromRelationByToAndFromField(project = project, relationId = relation.id, aField = aField, aId = aId, bField = bField, bId = bId)

    // note: for relations between same model, same field we add a relation row for both directions
    if (aField == bField) {
      sqlMutactions :+=
        RemoveDataItemFromRelationByToAndFromField(project = project, relationId = relation.id, aField = bField, aId = bId, bField = aField, bId = aId)
    }

    val transactionMutaction = Transaction(sqlMutactions, dataResolver)

    Future.successful(
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false),
        // dummy mutaction group for actions to satisfy tests. Please implement actions :-)
        MutactionGroup(mutactions = List(), async = true)
      ))
  }

  override def getReturnValue: Future[ReturnValueResult] = returnValueById(fromModel, aId)

}