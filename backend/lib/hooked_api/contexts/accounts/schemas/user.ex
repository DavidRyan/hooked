defmodule HookedApi.Accounts.User do
  use Ecto.Schema
  import Ecto.Changeset

  @derive {Jason.Encoder,
           only: [
             :id,
             :email,
             :first_name,
             :last_name,
             :is_active,
             :home_lat,
             :home_lng,
             :target_species,
             :onboarding_completed,
             :inserted_at,
             :updated_at
           ]}
  @primary_key {:id, :binary_id, autogenerate: true}
  @foreign_key_type :binary_id

  @type t :: %__MODULE__{
          id: binary(),
          email: String.t(),
          password_hash: String.t(),
          first_name: String.t() | nil,
          last_name: String.t() | nil,
          is_active: boolean(),
          failed_login_attempts: integer(),
          locked_until: DateTime.t() | nil,
          last_failed_login: DateTime.t() | nil,
          home_lat: float() | nil,
          home_lng: float() | nil,
          target_species: list(String.t()),
          onboarding_completed: boolean(),
          inserted_at: DateTime.t(),
          updated_at: DateTime.t()
        }

  schema "users" do
    field(:email, :string)
    field(:password_hash, :string)
    field(:password, :string, virtual: true, redact: true)
    field(:first_name, :string)
    field(:last_name, :string)
    field(:is_active, :boolean, default: true)
    field(:failed_login_attempts, :integer, default: 0)
    field(:locked_until, :utc_datetime)
    field(:last_failed_login, :utc_datetime)
    field(:home_lat, :float)
    field(:home_lng, :float)
    field(:target_species, {:array, :string}, default: [])
    field(:onboarding_completed, :boolean, default: false)
    has_many(:catches, HookedApi.Catches.UserCatch)
    has_many(:skunks, HookedApi.Skunks.UserSkunk)

    timestamps(type: :utc_datetime)
  end

  def registration_changeset(user, attrs) do
    user
    |> cast(attrs, [:email, :password, :first_name, :last_name])
    |> validate_required([:email, :password])
    |> validate_email()
    |> validate_password()
    |> hash_password()
  end

  def login_changeset(user, attrs) do
    user
    |> cast(attrs, [:email, :password])
    |> validate_required([:email, :password])
    |> validate_email()
  end

  def update_changeset(user, attrs) do
    user
    |> cast(attrs, [
      :first_name,
      :last_name,
      :is_active,
      :failed_login_attempts,
      :locked_until,
      :last_failed_login,
      :home_lat,
      :home_lng,
      :target_species,
      :onboarding_completed
    ])
    |> validate_length(:first_name, min: 1, max: 50)
    |> validate_length(:last_name, min: 1, max: 50)
    |> validate_number(:failed_login_attempts, greater_than_or_equal_to: 0)
    |> validate_number(:home_lat, greater_than_or_equal_to: -90, less_than_or_equal_to: 90)
    |> validate_number(:home_lng, greater_than_or_equal_to: -180, less_than_or_equal_to: 180)
  end

  defp validate_email(changeset) do
    changeset
    |> validate_required([:email])
    |> validate_format(:email, ~r/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
      message: "must be a valid email address"
    )
    |> validate_length(:email, max: 160)
    |> unsafe_validate_unique(:email, HookedApi.Repo)
    |> unique_constraint(:email)
  end

  defp validate_password(changeset) do
    changeset
    |> validate_required([:password])
    |> validate_length(:password, min: 8, max: 72)
    |> validate_format(:password, ~r/[a-z]/, message: "at least one lower case character")
    |> validate_format(:password, ~r/[A-Z]/, message: "at least one upper case character")
    |> validate_format(:password, ~r/[!?@#$%^&*_0-9]/,
      message: "at least one digit or punctuation character"
    )
  end

  defp hash_password(changeset) do
    password = get_change(changeset, :password)

    if password && changeset.valid? do
      changeset
      |> delete_change(:password)
      |> put_change(:password_hash, Bcrypt.hash_pwd_salt(password))
    else
      changeset
    end
  end

  def valid_password?(%__MODULE__{password_hash: hash}, password)
      when is_binary(hash) and byte_size(password) > 0 do
    Bcrypt.verify_pass(password, hash)
  end

  def valid_password?(_, _) do
    Bcrypt.no_user_verify()
    false
  end
end
